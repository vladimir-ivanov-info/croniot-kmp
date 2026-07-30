package com.croniot.client.data.source.sensors

import com.croniot.client.data.source.local.database.daos.SensorDataDao
import com.croniot.client.data.source.local.database.entities.SensorDataEntity
import com.croniot.client.domain.models.SensorData
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class LocalSensorDataSourceRoomImplTest {

    private val sensorDataDao: SensorDataDao = mockk()
    private val dataSource = LocalSensorDataSourceRoomImpl(sensorDataDao)

    @Test
    fun `save maps SensorData to entity and inserts it`() = runTest {
        val timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(5000L), ZoneOffset.UTC)
        val sensorData = SensorData(deviceUuid = "device-1", sensorTypeUid = 10L, value = "25.5", timeStamp = timestamp)
        coJustRun { sensorDataDao.insert(any()) }

        dataSource.save(sensorData)

        coVerify(exactly = 1) {
            sensorDataDao.insert(
                SensorDataEntity(deviceUuid = "device-1", sensorTypeUid = 10L, value = "25.5", timeStampMillis = 5000L),
            )
        }
    }

    @Test
    fun `getLatestSensorData maps entities to domain models`() = runTest {
        val entity = SensorDataEntity(deviceUuid = "device-1", sensorTypeUid = 10L, value = "20.0", timeStampMillis = 1000L)
        coEvery { sensorDataDao.getLatest("device-1", 10L, 5) } returns listOf(entity)

        val result = dataSource.getLatestSensorData("device-1", 10L, 5)

        assertEquals(1, result.size)
        assertEquals("20.0", result.first().value)
        assertEquals(1000L, result.first().timeStamp.toInstant().toEpochMilli())
    }

    @Test
    fun `getLatestSensorData returns empty list when dao has no data`() = runTest {
        coEvery { sensorDataDao.getLatest(any(), any(), any()) } returns emptyList()

        val result = dataSource.getLatestSensorData("device-1", 10L, 5)

        assertEquals(emptyList<SensorData>(), result)
    }

    @Test
    fun `observeSensorData maps non-null entity emissions to domain models`() = runTest {
        val entity = SensorDataEntity(deviceUuid = "device-1", sensorTypeUid = 10L, value = "22.0", timeStampMillis = 2000L)
        every { sensorDataDao.observeLatest("device-1", 10L) } returns flowOf(entity)

        val result = dataSource.observeSensorData("device-1", 10L).first()

        assertEquals("22.0", result.value)
    }

    @Test
    fun `observeSensorData filters out null emissions from the dao`() = runTest {
        val entity = SensorDataEntity(deviceUuid = "device-1", sensorTypeUid = 10L, value = "22.0", timeStampMillis = 2000L)
        every { sensorDataDao.observeLatest("device-1", 10L) } returns flowOf(null, entity)

        val result = dataSource.observeSensorData("device-1", 10L).first()

        assertEquals("22.0", result.value)
    }
}
