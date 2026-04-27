package com.croniot.client.data.repositories

import Outcome
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.croniot.client.data.source.local.ble.BleCredentialStore
import com.croniot.client.data.source.local.database.daos.BleKnownDeviceDao
import com.croniot.client.data.source.local.database.entities.BleKnownDeviceEntity
import com.croniot.client.data.source.remote.ble.BleConnectionPool
import com.croniot.client.data.source.remote.ble.BleScanResult
import com.croniot.client.data.source.remote.ble.BleScanner
import com.croniot.client.data.source.transport.TransportRouter
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.TransportKind
import com.croniot.client.domain.models.ble.DiscoveredBleDevice
import com.croniot.client.domain.models.ble.KnownBleDevice
import com.croniot.client.domain.repositories.BleDevicesRepository
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
                bleKnownDeviceDao.upsert(
                    BleKnownDeviceEntity(
                        uuid = deviceUuid,
                        displayName = scanResult.advertisedName ?: deviceUuid,
                        macAddress = scanResult.macAddress,
                        lastSeenAtMillis = now,
                        addedAtMillis = now,
                    ),
                )
                transportRouter.markBle(deviceUuid)
                Outcome.Ok(toDevice(deviceUuid, scanResult.advertisedName))
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
                Outcome.Ok(toDevice(deviceUuid, entity.displayName))
            }
            is Outcome.Err -> Outcome.Err(result.error)
        }
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

    private fun adapterOrNull(): BluetoothAdapter? {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return mgr?.adapter?.takeIf { it.isEnabled }
    }

    private fun toDevice(uuid: String, displayName: String?): Device = Device(
        uuid = uuid,
        name = displayName ?: uuid,
        description = "",
        transport = TransportKind.BLE,
    )
}
