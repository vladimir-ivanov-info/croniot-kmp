package com.croniot.client.data.source.sensors

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.domain.models.SensorData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZonedDateTime

/**
 * Complements LocalSensorDataSourceRoomImplTest (mocked SensorDataDao) with an end-to-end pass
 * through a real in-memory Room database, verifying the entity<->domain mapping and the DAO
 * queries work together correctly, not just in isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalSensorDataSourceRoomImplIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var dataSource: LocalSensorDataSourceRoomImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataSource = LocalSensorDataSourceRoomImpl(db.sensorDataDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `save then getLatestSensorData returns the reading mapped back to a domain model`() = runTest {
        val reading = SensorData(
            deviceUuid = "device-1",
            sensorTypeUid = 10L,
            value = "23.5",
            timeStamp = ZonedDateTime.now().withNano(0),
        )

        dataSource.save(reading)
        val result = dataSource.getLatestSensorData("device-1", 10L, 5)

        assertEquals(1, result.size)
        assertEquals(reading.value, result.first().value)
        assertEquals(reading.timeStamp.toInstant(), result.first().timeStamp.toInstant())
    }

    @Test
    fun `save multiple readings then getLatestSensorData returns them newest first`() = runTest {
        dataSource.save(SensorData("device-1", 10L, "20.0", ZonedDateTime.now().minusMinutes(2)))
        dataSource.save(SensorData("device-1", 10L, "22.0", ZonedDateTime.now().minusMinutes(1)))
        dataSource.save(SensorData("device-1", 10L, "25.0", ZonedDateTime.now()))

        val result = dataSource.getLatestSensorData("device-1", 10L, 10)

        assertEquals(listOf("25.0", "22.0", "20.0"), result.map { it.value })
    }

    @Test
    fun `observeSensorData emits the saved reading mapped to a domain model`() = runTest {
        val reading = SensorData("device-1", 10L, "18.0", ZonedDateTime.now())
        dataSource.save(reading)

        val result = dataSource.observeSensorData("device-1", 10L).first()

        assertEquals("18.0", result.value)
    }
}
