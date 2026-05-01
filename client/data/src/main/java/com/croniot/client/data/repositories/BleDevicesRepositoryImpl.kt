package com.croniot.client.data.repositories

import Outcome
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.croniot.client.data.source.local.ble.BleCredentialStore
import com.croniot.client.data.source.local.database.daos.BleKnownDeviceDao
import com.croniot.client.data.source.local.database.entities.BleKnownDeviceEntity
import android.util.Log
import com.croniot.client.data.source.remote.ble.BleConnection
import com.croniot.client.data.source.remote.ble.BleConnectionPool
import com.croniot.client.data.source.remote.ble.BleSchemaDto
import com.croniot.client.data.source.remote.ble.BleScanResult
import com.croniot.client.data.source.remote.ble.BleScanner
import com.croniot.client.data.source.remote.ble.BleSyncResult
import com.croniot.client.data.source.remote.mappers.toDomain
import com.croniot.client.data.source.transport.TransportRouter
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.SensorType
import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.models.TransportKind
import com.croniot.client.domain.models.ble.DiscoveredBleDevice
import com.croniot.client.domain.models.ble.KnownBleDevice
import com.croniot.client.domain.repositories.BleDevicesRepository
import croniot.messages.MessageFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

@SuppressLint("MissingPermission")
class BleDevicesRepositoryImpl(
    private val context: Context,
    private val scanner: BleScanner,
    private val connectionPool: BleConnectionPool,
    private val credentialStore: BleCredentialStore,
    private val bleKnownDeviceDao: BleKnownDeviceDao,
    private val transportRouter: TransportRouter,
    appScope: CoroutineScope,
) : BleDevicesRepository {

    // Caché transitoria UUID → ScanResult: necesaria para resolver MAC en pair() sin volver a scanear.
    private val recentScanByUuid = MutableStateFlow<Map<String, BleScanResult>>(emptyMap())

    private val sharedScan = scanner.scan()
        .onEach { results ->
            recentScanByUuid.value = results.associateBy { it.deviceUuid }
        }
        .shareIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 1_000L),
            replay = 1,
        )

    override fun observeNearbyDevices(): Flow<List<DiscoveredBleDevice>> = sharedScan
        .map { results ->
            val pairedUuids = bleKnownDeviceDao.getAllUuids().toSet()
            results.map { sr ->
                DiscoveredBleDevice(
                    uuid = sr.deviceUuid,
                    displayName = sr.advertisedName ?: sr.deviceUuid,
                    rssi = sr.rssi,
                    isPaired = sr.deviceUuid in pairedUuids,
                )
            }
        }

    override fun observeKnownDevices(): Flow<List<KnownBleDevice>> = combine(
        bleKnownDeviceDao.observeAll(),
        sharedScan.onStart { emit(emptyList()) },
    ) { entities, scanned ->
        val nearbyUuids = scanned.map { it.deviceUuid }.toSet()
        entities.map { entity ->
            KnownBleDevice(
                uuid = entity.uuid,
                displayName = entity.displayName,
                lastSeenAtMillis = entity.lastSeenAtMillis,
                isInRange = entity.uuid in nearbyUuids,
            )
        }
    }

    override suspend fun pair(
        deviceUuid: String,
        username: String,
        password: String,
    ): Outcome<Device, BleError> {
        val scanResult = recentScanByUuid.value[deviceUuid]
            ?: return Outcome.Err(BleError.NotFound(deviceUuid))
        val adapter = adapterOrNull() ?: return Outcome.Err(BleError.BluetoothOff)
        val btDevice = adapter.getRemoteDevice(scanResult.macAddress)

        return when (val result = connectionPool.getOrConnect(deviceUuid, btDevice, username, password)) {
            is Outcome.Ok -> {
                credentialStore.save(deviceUuid, username, password)
                val now = System.currentTimeMillis()
                val displayName = scanResult.advertisedName ?: deviceUuid
                bleKnownDeviceDao.upsert(
                    BleKnownDeviceEntity(
                        uuid = deviceUuid,
                        displayName = displayName,
                        macAddress = scanResult.macAddress,
                        lastSeenAtMillis = now,
                        addedAtMillis = now,
                    ),
                )
                transportRouter.markBle(deviceUuid)
                val device = syncAndBuildDevice(result.value, deviceUuid, displayName, cachedSchemaVersion = null)
                Outcome.Ok(device)
            }
            is Outcome.Err -> Outcome.Err(result.error)
        }
    }

    override suspend fun connect(deviceUuid: String): Outcome<Device, BleError> {
        val entity = bleKnownDeviceDao.getByUuid(deviceUuid)
            ?: return Outcome.Err(BleError.NotFound(deviceUuid))
        val credentials = credentialStore.get(deviceUuid)
            ?: return Outcome.Err(BleError.RequiresPairing)
        val adapter = adapterOrNull() ?: return Outcome.Err(BleError.BluetoothOff)
        val btDevice = adapter.getRemoteDevice(entity.macAddress)

        return when (val result = connectionPool.getOrConnect(
            deviceUuid = deviceUuid,
            device = btDevice,
            username = credentials.username,
            password = credentials.password,
        )) {
            is Outcome.Ok -> {
                bleKnownDeviceDao.touchLastSeen(deviceUuid, System.currentTimeMillis())
                transportRouter.markBle(deviceUuid)
                val cachedVersion = entity.schemaVersion.takeIf { it != 0L }
                val device = syncAndBuildDevice(result.value, deviceUuid, entity.displayName, cachedVersion)
                Outcome.Ok(device)
            }
            is Outcome.Err -> Outcome.Err(result.error)
        }
    }

    override suspend fun getDevice(deviceUuid: String): Device? {
        val entity = bleKnownDeviceDao.getByUuid(deviceUuid) ?: return null
        val (sensorTypes, taskTypes) = parseSchema(entity.schemaJson)
        return toDevice(entity.uuid, entity.displayName, sensorTypes, taskTypes)
    }

    override suspend fun forget(deviceUuid: String) {
        connectionPool.close(deviceUuid)
        credentialStore.forget(deviceUuid)
        bleKnownDeviceDao.delete(deviceUuid)
        transportRouter.markCloud(deviceUuid)
    }

    override suspend fun disconnectAll() {
        connectionPool.closeAll()
    }

    private suspend fun syncAndBuildDevice(
        connection: BleConnection,
        deviceUuid: String,
        displayName: String?,
        cachedSchemaVersion: Long?,
    ): Device {
        val sensorTypes: List<SensorType>
        val taskTypes: List<TaskType>
        when (val outcome = connection.syncSchema(cachedSchemaVersion)) {
            is Outcome.Ok -> when (val sync = outcome.value) {
                is BleSyncResult.Updated -> {
                    bleKnownDeviceDao.updateSchema(deviceUuid, sync.schemaVersion, sync.schemaJson)
                    val parsed = parseSchema(sync.schemaJson)
                    sensorTypes = parsed.first
                    taskTypes = parsed.second
                }
                BleSyncResult.UpToDate -> {
                    val entity = bleKnownDeviceDao.getByUuid(deviceUuid)
                    val parsed = parseSchema(entity?.schemaJson)
                    sensorTypes = parsed.first
                    taskTypes = parsed.second
                }
            }
            is Outcome.Err -> {
                Log.w("BleDevicesRepo", "Schema sync failed: ${outcome.error}")
                sensorTypes = emptyList()
                taskTypes = emptyList()
            }
        }
        return toDevice(deviceUuid, displayName, sensorTypes, taskTypes)
    }

    private fun parseSchema(json: String?): Pair<List<SensorType>, List<TaskType>> {
        if (json == null) return Pair(emptyList(), emptyList())
        return runCatching {
            val dto = MessageFactory.fromJson<BleSchemaDto>(json)
            Pair(
                dto.sensorTypes.map { it.toDomain() },
                dto.taskTypes.map { it.toDomain() },
            )
        }.getOrElse { Pair(emptyList(), emptyList()) }
    }

    private fun adapterOrNull(): BluetoothAdapter? {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return mgr?.adapter?.takeIf { it.isEnabled }
    }

    private fun toDevice(
        uuid: String,
        displayName: String?,
        sensorTypes: List<SensorType>,
        taskTypes: List<TaskType>,
    ): Device = Device(
        uuid = uuid,
        name = displayName ?: uuid,
        description = "",
        transport = TransportKind.BLE,
        sensorTypes = sensorTypes,
        taskTypes = taskTypes,
    )
}
