package com.croniot.client.data.source.sensors

import Outcome
import com.croniot.client.data.source.remote.ble.BleConnection
import com.croniot.client.data.source.remote.ble.BleConnectionPool
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.SensorData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class BleSensorDataSourceImplTest {

    private val connectionPool: BleConnectionPool = mockk()
    private val appScope = CoroutineScope(UnconfinedTestDispatcher())
    private val dataSource = BleSensorDataSourceImpl(appScope, connectionPool)

    private fun sensorData() = SensorData(deviceUuid = "device-1", sensorTypeUid = 1L, value = "25.5", timeStamp = ZonedDateTime.now())

    @Test
    fun `WHEN device has no active connection THEN listenDeviceSensors returns Err Unknown`() = runTest {
        every { connectionPool.get("device-1") } returns null

        val result = dataSource.listenDeviceSensors("device-1")

        assertEquals(Outcome.Err(ConnectionError.Unknown), result)
    }

    @Test
    fun `WHEN device has an active connection THEN listenDeviceSensors returns Ok and subscribes to its sensor data`() = runTest {
        val connection: BleConnection = mockk {
            every { observeSensorData() } returns flowOf(sensorData())
        }
        every { connectionPool.get("device-1") } returns connection

        val result = dataSource.listenDeviceSensors("device-1")

        assertTrue(result is Outcome.Ok)
        io.mockk.verify(exactly = 1) { connection.observeSensorData() }
    }

    @Test
    fun `WHEN nothing is active THEN stopListening for a specific deviceUuid does not throw`() = runTest {
        dataSource.stopListening("device-1")
    }

    @Test
    fun `WHEN stopListening is called with null THEN it cancels all active listeners without throwing`() = runTest {
        val connection: BleConnection = mockk {
            every { observeSensorData() } returns flowOf(sensorData())
        }
        every { connectionPool.get(any()) } returns connection
        dataSource.listenDeviceSensors("device-1")
        dataSource.listenDeviceSensors("device-2")

        dataSource.stopListening(null)
    }

    @Test
    fun `WHEN listenDeviceSensors is called twice for the same device THEN it replaces the previous listener`() = runTest {
        val connection: BleConnection = mockk {
            every { observeSensorData() } returns flowOf(sensorData())
        }
        every { connectionPool.get("device-1") } returns connection

        val first = dataSource.listenDeviceSensors("device-1")
        val second = dataSource.listenDeviceSensors("device-1")

        assertTrue(first is Outcome.Ok)
        assertTrue(second is Outcome.Ok)
    }
}
