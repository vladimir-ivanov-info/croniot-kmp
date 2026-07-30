package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.BleKnownDeviceEntity
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
class BleKnownDeviceDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BleKnownDeviceDao

    private fun entity(uuid: String, lastSeen: Long = 1000L) = BleKnownDeviceEntity(
        uuid = uuid,
        displayName = "Device $uuid",
        macAddress = "AA:BB:CC:00:00:01",
        lastSeenAtMillis = lastSeen,
        addedAtMillis = 500L,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.bleKnownDeviceDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `upsert then getByUuid returns the persisted device`() = runTest {
        dao.upsert(entity("device-1"))

        val result = dao.getByUuid("device-1")

        assertEquals("Device device-1", result?.displayName)
    }

    @Test
    fun `getByUuid returns null for an unknown uuid`() = runTest {
        assertNull(dao.getByUuid("unknown"))
    }

    @Test
    fun `upsert with the same uuid replaces the existing row`() = runTest {
        dao.upsert(entity("device-1").copy(displayName = "Old Name"))
        dao.upsert(entity("device-1").copy(displayName = "New Name"))

        val result = dao.getByUuid("device-1")

        assertEquals("New Name", result?.displayName)
    }

    @Test
    fun `observeAll emits devices ordered by lastSeenAtMillis descending`() = runTest {
        dao.upsert(entity("device-1", lastSeen = 1000L))
        dao.upsert(entity("device-2", lastSeen = 3000L))
        dao.upsert(entity("device-3", lastSeen = 2000L))

        val result = dao.observeAll().first()

        assertEquals(listOf("device-2", "device-3", "device-1"), result.map { it.uuid })
    }

    @Test
    fun `getAllUuids returns every stored uuid`() = runTest {
        dao.upsert(entity("device-1"))
        dao.upsert(entity("device-2"))

        val result = dao.getAllUuids()

        assertEquals(setOf("device-1", "device-2"), result.toSet())
    }

    @Test
    fun `touchLastSeen updates only the lastSeenAtMillis field`() = runTest {
        dao.upsert(entity("device-1", lastSeen = 1000L))

        dao.touchLastSeen("device-1", 5000L)

        val result = dao.getByUuid("device-1")
        assertEquals(5000L, result?.lastSeenAtMillis)
        assertEquals("Device device-1", result?.displayName)
    }

    @Test
    fun `updateSchema sets schemaVersion and schemaJson`() = runTest {
        dao.upsert(entity("device-1"))

        dao.updateSchema("device-1", 3L, "{\"sensorTypes\":[]}")

        val result = dao.getByUuid("device-1")
        assertEquals(3L, result?.schemaVersion)
        assertEquals("{\"sensorTypes\":[]}", result?.schemaJson)
    }

    @Test
    fun `delete removes the device`() = runTest {
        dao.upsert(entity("device-1"))

        dao.delete("device-1")

        assertNull(dao.getByUuid("device-1"))
    }

    @Test
    fun `observeAll emits an empty list when no devices are known`() = runTest {
        assertTrue(dao.observeAll().first().isEmpty())
    }
}
