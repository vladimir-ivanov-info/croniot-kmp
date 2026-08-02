package com.croniot.client.data.repositories

import Outcome
import com.croniot.client.data.source.sensors.LocalSensorDataSource
import com.croniot.client.data.source.sensors.RemoteSensorDataSource
import com.croniot.client.data.source.transport.TransportRouter
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.SensorData
import com.croniot.client.domain.models.TransportKind
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class SensorDataRepositoryImplTest {

    private val cloudSensorDataSource: RemoteSensorDataSource = mockk()
    private val bleSensorDataSource: RemoteSensorDataSource = mockk()
    private val transportRouter: TransportRouter = mockk()
    private val localSensorDataSource: LocalSensorDataSource = mockk()
    private val scope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    private lateinit var repository: SensorDataRepositoryImpl

    private val device = Device(
        uuid = "device-uuid",
        name = "device-name",
        description = "device-description",
    )

    private val sensorData = SensorData(
        deviceUuid = device.uuid,
        sensorTypeUid = 1L,
        value = "23.5",
        timeStamp = ZonedDateTime.now(),
    )

    @BeforeEach
    fun setUp() {
        repository = SensorDataRepositoryImpl(
            cloudSensorDataSource = cloudSensorDataSource,
            bleSensorDataSource = bleSensorDataSource,
            transportRouter = transportRouter,
            localSensorDataSource = localSensorDataSource,
            scope = scope,
        )
    }

    @Test
    fun `WHEN routing is CLOUD THEN stopListeningFor delegates to cloud data source`() = runTest {
        every { transportRouter.transportFor(device.uuid) } returns TransportKind.CLOUD
        coJustRun { cloudSensorDataSource.stopListening(any()) }

        repository.stopListeningFor(device.uuid)

        coVerify(exactly = 1) { cloudSensorDataSource.stopListening(device.uuid) }
        coVerify(exactly = 0) { bleSensorDataSource.stopListening(any()) }
    }

    @Test
    fun `WHEN routing is BLE THEN stopListeningFor delegates to ble data source`() = runTest {
        every { transportRouter.transportFor(device.uuid) } returns TransportKind.BLE
        coJustRun { bleSensorDataSource.stopListening(any()) }

        repository.stopListeningFor(device.uuid)

        coVerify(exactly = 1) { bleSensorDataSource.stopListening(device.uuid) }
        coVerify(exactly = 0) { cloudSensorDataSource.stopListening(any()) }
    }

    @Test
    fun `WHEN stopListeningFor is called THEN it removes the device entry from devicesLatestSensorTimestamp`() = runTest {
        every { transportRouter.transportFor(device.uuid) } returns TransportKind.CLOUD
        coEvery { cloudSensorDataSource.listenDeviceSensors(device.uuid) } returns
            Outcome.Ok(flowOf(sensorData))
        coJustRun { localSensorDataSource.save(any()) }
        coJustRun { cloudSensorDataSource.stopListening(any()) }

        repository.listenToDeviceSensors(device)
        assertTrue(repository.devicesLatestSensorTimestamp.value.containsKey(device.uuid))

        repository.stopListeningFor(device.uuid)

        assertTrue(repository.devicesLatestSensorTimestamp.value.isEmpty())
    }

    @Test
    fun `WHEN stopAllListeners is called THEN it stops both data sources and clears devicesLatestSensorTimestamp`() = runTest {
        coJustRun { cloudSensorDataSource.stopListening(null) }
        coJustRun { bleSensorDataSource.stopListening(null) }

        repository.stopAllListeners()

        coVerify(exactly = 1) { cloudSensorDataSource.stopListening(deviceUuid = null) }
        coVerify(exactly = 1) { bleSensorDataSource.stopListening(deviceUuid = null) }
        assertTrue(repository.devicesLatestSensorTimestamp.value.isEmpty())
    }

    @Test
    fun `WHEN data source resolved by routing succeeds THEN listenToDeviceSensors returns Ok`() = runTest {
        every { transportRouter.transportFor(device.uuid) } returns TransportKind.CLOUD
        coEvery { cloudSensorDataSource.listenDeviceSensors(device.uuid) } returns
            Outcome.Ok(flowOf(sensorData))
        coJustRun { localSensorDataSource.save(any()) }

        val result = repository.listenToDeviceSensors(device)

        assertEquals(Outcome.Ok(Unit), result)
    }

    @Test
    fun `WHEN data source resolved by routing returns Err THEN listenToDeviceSensors propagates it`() = runTest {
        every { transportRouter.transportFor(device.uuid) } returns TransportKind.BLE
        coEvery { bleSensorDataSource.listenDeviceSensors(device.uuid) } returns
            Outcome.Err(ConnectionError.Unknown)

        val result = repository.listenToDeviceSensors(device)

        assertEquals(Outcome.Err(ConnectionError.Unknown), result)
    }

    @Test
    fun `WHEN sensor readings are emitted THEN listenToDeviceSensors saves each to the local data source`() = runTest {
        every { transportRouter.transportFor(device.uuid) } returns TransportKind.CLOUD
        coEvery { cloudSensorDataSource.listenDeviceSensors(device.uuid) } returns
            Outcome.Ok(flowOf(sensorData))
        coJustRun { localSensorDataSource.save(any()) }

        repository.listenToDeviceSensors(device)

        coVerify(exactly = 1) { localSensorDataSource.save(sensorData) }
    }

    @Test
    fun `WHEN getLatestSensorData is called THEN it delegates to local data source`() = runTest {
        coEvery { localSensorDataSource.getLatestSensorData(device.uuid, 1L, 10) } returns listOf(sensorData)

        val result = repository.getLatestSensorData(device.uuid, 1L, 10)

        assertEquals(listOf(sensorData), result)
    }

    @Test
    fun `WHEN observeSensorData is called THEN it delegates to local data source`() = runTest {
        every { localSensorDataSource.observeSensorData(device.uuid, 1L) } returns flowOf(sensorData)

        val result = repository.observeSensorData(device.uuid, 1L)

        assertEquals(sensorData, result.first())
    }
}
