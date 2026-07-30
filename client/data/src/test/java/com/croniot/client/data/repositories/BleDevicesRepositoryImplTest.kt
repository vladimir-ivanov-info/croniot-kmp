package com.croniot.client.data.repositories

import Outcome
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.croniot.client.data.source.local.ble.BleCredentialStore
import com.croniot.client.data.source.local.ble.BleCredentials
import com.croniot.client.data.source.local.database.daos.BleKnownDeviceDao
import com.croniot.client.data.source.local.database.entities.BleKnownDeviceEntity
import com.croniot.client.data.source.remote.ble.BleConnection
import com.croniot.client.data.source.remote.ble.BleConnectionPool
import com.croniot.client.data.source.remote.ble.BleSchemaDto
import com.croniot.client.data.source.remote.ble.BleScanner
import com.croniot.client.data.source.remote.ble.BleSyncResult
import com.croniot.client.data.source.transport.TransportRouter
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.TransportKind
import croniot.messages.MessageFactory
import croniot.models.dto.ParameterSensorDto
import croniot.models.dto.ParameterTaskDto
import croniot.models.dto.SensorTypeDto
import croniot.models.dto.TaskTypeDto
import com.croniot.client.data.source.remote.ble.BleScanResult
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BleDevicesRepositoryImplTest {

    private val context: Context = mockk(relaxed = true)
    private val scanner: BleScanner = mockk()
    private val connectionPool: BleConnectionPool = mockk()
    private val credentialStore: BleCredentialStore = mockk()
    private val bleKnownDeviceDao: BleKnownDeviceDao = mockk()
    private val transportRouter: TransportRouter = mockk()
    private val appScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private lateinit var repository: BleDevicesRepositoryImpl

    private fun entity(uuid: String, schemaJson: String? = null) = BleKnownDeviceEntity(
        uuid = uuid,
        displayName = "display-$uuid",
        macAddress = "AA:BB:CC:DD:EE:FF",
        lastSeenAtMillis = 1_000L,
        addedAtMillis = 500L,
        schemaJson = schemaJson,
    )

    @BeforeEach
    fun setUp() {
        every { scanner.scan() } returns emptyFlow()

        repository = BleDevicesRepositoryImpl(
            context = context,
            scanner = scanner,
            connectionPool = connectionPool,
            credentialStore = credentialStore,
            bleKnownDeviceDao = bleKnownDeviceDao,
            transportRouter = transportRouter,
            appScope = appScope,
        )
    }

    @Test
    fun `getDevice returns null when entity is not found`() = runTest {
        coEvery { bleKnownDeviceDao.getByUuid("unknown-uuid") } returns null

        val result = repository.getDevice("unknown-uuid")

        assertNull(result)
    }

    @Test
    fun `getDevice returns device with empty schema lists when schemaJson is null`() = runTest {
        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity("device-uuid", schemaJson = null)

        val result = repository.getDevice("device-uuid")

        assertEquals("device-uuid", result?.uuid)
        assertEquals("display-device-uuid", result?.name)
        assertEquals(TransportKind.BLE, result?.transport)
        assertTrue(result?.sensorTypes.isNullOrEmpty())
        assertTrue(result?.taskTypes.isNullOrEmpty())
    }

    @Test
    fun `getDevice maps schemaJson sensorTypes and taskTypes to domain`() = runTest {
        val sensorTypeDto = SensorTypeDto(
            uid = 1L,
            name = "temperature",
            description = "temp sensor",
            parameters = listOf(
                ParameterSensorDto(
                    uid = 1L,
                    name = "threshold",
                    type = "double",
                    unit = "celsius",
                    description = "threshold",
                    constraints = emptyMap(),
                ),
            ),
        )
        val taskTypeDto = TaskTypeDto(
            uid = 2L,
            name = "turn on",
            description = "turn on relay",
            parameters = listOf(
                ParameterTaskDto(
                    uid = 2L,
                    name = "duration",
                    type = "int",
                    unit = "seconds",
                    description = "duration",
                    constraints = emptyMap(),
                ),
            ),
        )
        val schemaDto = BleSchemaDto(sensorTypes = listOf(sensorTypeDto), taskTypes = listOf(taskTypeDto))
        val schemaJson = MessageFactory.toJson(schemaDto)

        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity("device-uuid", schemaJson = schemaJson)

        val result = repository.getDevice("device-uuid")

        assertEquals(1, result?.sensorTypes?.size)
        assertEquals(sensorTypeDto.uid, result?.sensorTypes?.first()?.uid)
        assertEquals(sensorTypeDto.name, result?.sensorTypes?.first()?.name)
        assertEquals(1, result?.taskTypes?.size)
        assertEquals(taskTypeDto.uid, result?.taskTypes?.first()?.uid)
        assertEquals(taskTypeDto.name, result?.taskTypes?.first()?.name)
    }

    @Test
    fun `getDevice returns device with empty schema lists when schemaJson is corrupted`() = runTest {
        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity(
            "device-uuid",
            schemaJson = "this-is-not-valid-json",
        )

        val result = repository.getDevice("device-uuid")

        assertEquals("device-uuid", result?.uuid)
        assertTrue(result?.sensorTypes.isNullOrEmpty())
        assertTrue(result?.taskTypes.isNullOrEmpty())
    }

    @Test
    fun `forget closes connection, forgets credentials, deletes known device and marks cloud transport`() = runTest {
        coJustRun { connectionPool.close(any()) }
        coJustRun { credentialStore.forget(any()) }
        coJustRun { bleKnownDeviceDao.delete(any()) }
        coJustRun { transportRouter.markCloud(any()) }

        repository.forget("device-uuid")

        coVerify(exactly = 1) { connectionPool.close("device-uuid") }
        coVerify(exactly = 1) { credentialStore.forget("device-uuid") }
        coVerify(exactly = 1) { bleKnownDeviceDao.delete("device-uuid") }
        coVerify(exactly = 1) { transportRouter.markCloud("device-uuid") }
    }

    @Test
    fun `disconnectAll delegates to connection pool`() = runTest {
        coJustRun { connectionPool.closeAll() }

        repository.disconnectAll()

        coVerify(exactly = 1) { connectionPool.closeAll() }
    }

    @Test
    fun `pair returns NotFound when deviceUuid is not in the recent scan cache`() = runTest {
        val result = repository.pair("device-uuid", "user", "pass")

        assertTrue(result is Outcome.Err)
        assertTrue((result as Outcome.Err).error is BleError.NotFound)
    }

    @Test
    fun `connect returns NotFound when device is not known`() = runTest {
        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns null

        val result = repository.connect("device-uuid")

        assertEquals(Outcome.Err(BleError.NotFound("device-uuid")), result)
    }

    @Test
    fun `connect returns RequiresPairing when no credentials are stored`() = runTest {
        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity("device-uuid")
        coEvery { credentialStore.get("device-uuid") } returns null

        val result = repository.connect("device-uuid")

        assertEquals(Outcome.Err(BleError.RequiresPairing), result)
    }

    @Test
    fun `connect returns BluetoothOff when adapter is unavailable`() = runTest {
        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity("device-uuid")
        coEvery { credentialStore.get("device-uuid") } returns BleCredentials("user", "pass")
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns null

        val result = repository.connect("device-uuid")

        assertEquals(Outcome.Err(BleError.BluetoothOff), result)
    }

    @Test
    fun `connect returns BluetoothOff when adapter is disabled`() = runTest {
        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity("device-uuid")
        coEvery { credentialStore.get("device-uuid") } returns BleCredentials("user", "pass")
        val adapter: BluetoothAdapter = mockk { every { isEnabled } returns false }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        val result = repository.connect("device-uuid")

        assertEquals(Outcome.Err(BleError.BluetoothOff), result)
    }

    @Test
    fun `connect on success marks ble transport, touches last seen and syncs schema`() = runTest {
        val btDevice: BluetoothDevice = mockk()
        val adapter: BluetoothAdapter = mockk {
            every { isEnabled } returns true
            every { getRemoteDevice("AA:BB:CC:DD:EE:FF") } returns btDevice
        }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity("device-uuid")
        coEvery { credentialStore.get("device-uuid") } returns BleCredentials("user", "pass")

        val connection: BleConnection = mockk()
        coEvery {
            connectionPool.getOrConnect("device-uuid", btDevice, "user", "pass")
        } returns Outcome.Ok(connection)
        coEvery { connection.syncSchema(any()) } returns Outcome.Ok(BleSyncResult.UpToDate)
        coJustRun { bleKnownDeviceDao.touchLastSeen(any(), any()) }
        coJustRun { transportRouter.markBle(any()) }

        val result = repository.connect("device-uuid")

        assertTrue(result is Outcome.Ok)
        assertEquals(TransportKind.BLE, (result as Outcome.Ok).value.transport)
        coVerify(exactly = 1) { transportRouter.markBle("device-uuid") }
        coVerify(exactly = 1) { bleKnownDeviceDao.touchLastSeen("device-uuid", any()) }
    }

    @Test
    fun `connect propagates connection pool error`() = runTest {
        val btDevice: BluetoothDevice = mockk()
        val adapter: BluetoothAdapter = mockk {
            every { isEnabled } returns true
            every { getRemoteDevice(any<String>()) } returns btDevice
        }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity("device-uuid")
        coEvery { credentialStore.get("device-uuid") } returns BleCredentials("user", "pass")
        coEvery {
            connectionPool.getOrConnect(any(), any(), any(), any())
        } returns Outcome.Err<BleError>(BleError.Timeout)

        val result = repository.connect("device-uuid")

        assertEquals(Outcome.Err(BleError.Timeout), result)
    }

    @Test
    fun `connect on schema Updated persists the new schema and parses it`() = runTest {
        val btDevice: BluetoothDevice = mockk()
        val adapter: BluetoothAdapter = mockk {
            every { isEnabled } returns true
            every { getRemoteDevice(any<String>()) } returns btDevice
        }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity("device-uuid")
        coEvery { credentialStore.get("device-uuid") } returns BleCredentials("user", "pass")

        val connection: BleConnection = mockk()
        coEvery {
            connectionPool.getOrConnect(any(), any(), any(), any())
        } returns Outcome.Ok(connection)
        val schemaJson = MessageFactory.toJson(BleSchemaDto(sensorTypes = emptyList(), taskTypes = emptyList()))
        coEvery { connection.syncSchema(any()) } returns Outcome.Ok(BleSyncResult.Updated(schemaVersion = 3L, schemaJson = schemaJson))
        coJustRun { bleKnownDeviceDao.touchLastSeen(any(), any()) }
        coJustRun { bleKnownDeviceDao.updateSchema(any(), any(), any()) }
        coJustRun { transportRouter.markBle(any()) }

        val result = repository.connect("device-uuid")

        assertTrue(result is Outcome.Ok)
        coVerify(exactly = 1) { bleKnownDeviceDao.updateSchema("device-uuid", 3L, schemaJson) }
    }

    @Test
    fun `connect on schema UpToDate falls back to empty schema lists when the known device entity has since disappeared`() = runTest {
        // syncAndBuildDevice() re-reads the dao on the UpToDate path; if the row was deleted
        // concurrently between the initial lookup and this second read, entity is null here.
        val btDevice: BluetoothDevice = mockk()
        val adapter: BluetoothAdapter = mockk {
            every { isEnabled } returns true
            every { getRemoteDevice(any<String>()) } returns btDevice
        }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returnsMany listOf(entity("device-uuid"), null)
        coEvery { credentialStore.get("device-uuid") } returns BleCredentials("user", "pass")

        val connection: BleConnection = mockk()
        coEvery {
            connectionPool.getOrConnect(any(), any(), any(), any())
        } returns Outcome.Ok(connection)
        coEvery { connection.syncSchema(any()) } returns Outcome.Ok(BleSyncResult.UpToDate)
        coJustRun { bleKnownDeviceDao.touchLastSeen(any(), any()) }
        coJustRun { transportRouter.markBle(any()) }

        val result = repository.connect("device-uuid")

        assertTrue(result is Outcome.Ok)
        assertTrue((result as Outcome.Ok).value.sensorTypes.isEmpty())
        assertTrue(result.value.taskTypes.isEmpty())
    }

    @Test
    fun `connect on schema sync failure logs and returns device with empty schema lists`() = runTest {
        val btDevice: BluetoothDevice = mockk()
        val adapter: BluetoothAdapter = mockk {
            every { isEnabled } returns true
            every { getRemoteDevice(any<String>()) } returns btDevice
        }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        coEvery { bleKnownDeviceDao.getByUuid("device-uuid") } returns entity("device-uuid")
        coEvery { credentialStore.get("device-uuid") } returns BleCredentials("user", "pass")

        val connection: BleConnection = mockk()
        coEvery {
            connectionPool.getOrConnect(any(), any(), any(), any())
        } returns Outcome.Ok(connection)
        coEvery { connection.syncSchema(any()) } returns Outcome.Err(BleError.Timeout)
        coJustRun { bleKnownDeviceDao.touchLastSeen(any(), any()) }
        coJustRun { transportRouter.markBle(any()) }

        android.util.Log::class.let { io.mockk.mockkStatic(it) }
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        try {
            val result = repository.connect("device-uuid")

            assertTrue(result is Outcome.Ok)
            assertTrue((result as Outcome.Ok).value.sensorTypes.isEmpty())
            assertTrue(result.value.taskTypes.isEmpty())
        } finally {
            io.mockk.unmockkStatic(android.util.Log::class)
        }
    }

    @Test
    fun `pair saves credentials, upserts known device and marks ble transport`() = runTest {
        val scanResult = BleScanResult(macAddress = "AA:BB:CC:DD:EE:FF", advertisedName = "MyDevice", rssi = -50)
        every { scanner.scan() } returns flowOf(listOf(scanResult))
        coEvery { bleKnownDeviceDao.getAllUuids() } returns emptyList()

        val localRepo = BleDevicesRepositoryImpl(
            context = context,
            scanner = scanner,
            connectionPool = connectionPool,
            credentialStore = credentialStore,
            bleKnownDeviceDao = bleKnownDeviceDao,
            transportRouter = transportRouter,
            appScope = appScope,
        )
        localRepo.observeNearbyDevices().first()

        val btDevice: BluetoothDevice = mockk()
        val adapter: BluetoothAdapter = mockk {
            every { isEnabled } returns true
            every { getRemoteDevice("AA:BB:CC:DD:EE:FF") } returns btDevice
        }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        val connection: BleConnection = mockk()
        coEvery {
            connectionPool.getOrConnect("AA:BB:CC:DD:EE:FF", btDevice, "user", "pass")
        } returns Outcome.Ok(connection)
        coEvery { connection.syncSchema(null) } returns Outcome.Ok(BleSyncResult.UpToDate)
        coEvery { bleKnownDeviceDao.getByUuid("AA:BB:CC:DD:EE:FF") } returns entity("AA:BB:CC:DD:EE:FF")
        coJustRun { credentialStore.save(any(), any(), any()) }
        coJustRun { bleKnownDeviceDao.upsert(any()) }
        coJustRun { transportRouter.markBle(any()) }

        val result = localRepo.pair("AA:BB:CC:DD:EE:FF", "user", "pass")

        assertTrue(result is Outcome.Ok)
        coVerify(exactly = 1) { credentialStore.save("AA:BB:CC:DD:EE:FF", "user", "pass") }
        coVerify(exactly = 1) { bleKnownDeviceDao.upsert(any()) }
        coVerify(exactly = 1) { transportRouter.markBle("AA:BB:CC:DD:EE:FF") }
    }

    @Test
    fun `pair returns BluetoothOff when adapter is unavailable`() = runTest {
        val scanResult = BleScanResult(macAddress = "AA:BB:CC:DD:EE:FF", advertisedName = "MyDevice", rssi = -50)
        every { scanner.scan() } returns flowOf(listOf(scanResult))
        coEvery { bleKnownDeviceDao.getAllUuids() } returns emptyList()

        val localRepo = BleDevicesRepositoryImpl(
            context = context,
            scanner = scanner,
            connectionPool = connectionPool,
            credentialStore = credentialStore,
            bleKnownDeviceDao = bleKnownDeviceDao,
            transportRouter = transportRouter,
            appScope = appScope,
        )
        localRepo.observeNearbyDevices().first()
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns null

        val result = localRepo.pair("AA:BB:CC:DD:EE:FF", "user", "pass")

        assertEquals(Outcome.Err(BleError.BluetoothOff), result)
    }

    @Test
    fun `observeNearbyDevices maps scan results and flags paired devices`() = runTest {
        val scanResult = BleScanResult(macAddress = "AA:BB:CC:DD:EE:FF", advertisedName = "MyDevice", rssi = -50)
        every { scanner.scan() } returns flowOf(listOf(scanResult))
        coEvery { bleKnownDeviceDao.getAllUuids() } returns listOf("AA:BB:CC:DD:EE:FF")

        val localRepo = BleDevicesRepositoryImpl(
            context = context,
            scanner = scanner,
            connectionPool = connectionPool,
            credentialStore = credentialStore,
            bleKnownDeviceDao = bleKnownDeviceDao,
            transportRouter = transportRouter,
            appScope = appScope,
        )

        val result = localRepo.observeNearbyDevices().first()

        assertEquals(1, result.size)
        assertEquals("AA:BB:CC:DD:EE:FF", result.first().uuid)
        assertEquals("MyDevice", result.first().displayName)
        assertTrue(result.first().isPaired)
    }

    @Test
    fun `observeKnownDevices flags devices currently in scan range`() = runTest {
        every { scanner.scan() } returns flowOf(
            listOf(BleScanResult(macAddress = "AA:BB:CC:DD:EE:FF", advertisedName = "MyDevice", rssi = -50)),
        )
        every { bleKnownDeviceDao.observeAll() } returns flowOf(listOf(entity("AA:BB:CC:DD:EE:FF")))

        val localRepo = BleDevicesRepositoryImpl(
            context = context,
            scanner = scanner,
            connectionPool = connectionPool,
            credentialStore = credentialStore,
            bleKnownDeviceDao = bleKnownDeviceDao,
            transportRouter = transportRouter,
            appScope = appScope,
        )

        // combine()'s first emission pairs `entities` with sharedScan.onStart's synthetic empty
        // list (see production onStart { emit(emptyList()) }); drop it to reach the emission
        // combined with the real scan result.
        val result = localRepo.observeKnownDevices().drop(1).first()

        assertEquals(1, result.size)
        assertTrue(result.first().isInRange)
    }
}
