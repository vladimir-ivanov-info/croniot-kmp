package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.SensorDataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SensorDataDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SensorDataDao

    private fun entity(deviceUuid: String = "device-1", sensorTypeUid: Long = 1L, value: String, timeStampMillis: Long) =
        SensorDataEntity(deviceUuid = deviceUuid, sensorTypeUid = sensorTypeUid, value = value, timeStampMillis = timeStampMillis)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.sensorDataDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getLatest returns readings ordered by timestamp descending`() = runTest {
        dao.insert(entity(value = "20.0", timeStampMillis = 1000L))
        dao.insert(entity(value = "25.0", timeStampMillis = 3000L))
        dao.insert(entity(value = "22.0", timeStampMillis = 2000L))

        val result = dao.getLatest("device-1", 1L, limit = 10)

        assertEquals(listOf("25.0", "22.0", "20.0"), result.map { it.value })
    }

    @Test
    fun `getLatest respects the limit parameter`() = runTest {
        repeat(5) { i -> dao.insert(entity(value = "v$i", timeStampMillis = i.toLong())) }

        val result = dao.getLatest("device-1", 1L, limit = 2)

        assertEquals(2, result.size)
    }

    @Test
    fun `getLatest only returns readings for the requested device and sensor type`() = runTest {
        dao.insert(entity(deviceUuid = "device-1", sensorTypeUid = 1L, value = "a", timeStampMillis = 1000L))
        dao.insert(entity(deviceUuid = "device-2", sensorTypeUid = 1L, value = "b", timeStampMillis = 1000L))
        dao.insert(entity(deviceUuid = "device-1", sensorTypeUid = 2L, value = "c", timeStampMillis = 1000L))

        val result = dao.getLatest("device-1", 1L, limit = 10)

        assertEquals(1, result.size)
        assertEquals("a", result.first().value)
    }

    @Test
    fun `getLatest returns an empty list when there is no data`() = runTest {
        assertTrue(dao.getLatest("device-1", 1L, limit = 10).isEmpty())
    }

    @Test
    fun `observeLatest emits the single most recent reading`() = runTest {
        dao.insert(entity(value = "20.0", timeStampMillis = 1000L))
        dao.insert(entity(value = "25.0", timeStampMillis = 2000L))

        val latest = dao.observeLatest("device-1", 1L).first()

        assertEquals("25.0", latest?.value)
    }

    @Test
    fun `observeLatest emits null when there is no data`() = runTest {
        assertNull(dao.observeLatest("device-1", 1L).first())
    }
}
