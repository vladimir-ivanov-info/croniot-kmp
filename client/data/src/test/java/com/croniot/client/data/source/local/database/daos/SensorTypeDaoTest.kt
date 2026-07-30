package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.AccountEntity
import com.croniot.client.data.source.local.database.entities.DeviceEntity
import com.croniot.client.data.source.local.database.entities.SensorTypeEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SensorTypeDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var sensorTypeDao: SensorTypeDao
    private var deviceId: Long = 0

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sensorTypeDao = db.sensorTypeDao()
        val accountId = db.accountDao().insert(AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com"))
        deviceId = db.deviceDao().insert(DeviceEntity(uuid = "device-1", accountId = accountId, name = "Device", description = ""))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert then getByUid returns the persisted sensor type`() = runTest {
        sensorTypeDao.insert(SensorTypeEntity(uid = 1L, deviceId = deviceId, name = "Temperature", description = "desc"))

        val result = sensorTypeDao.getByUid(1L)

        assertEquals("Temperature", result?.name)
    }

    @Test
    fun `getByUid returns null for an unknown uid`() = runTest {
        assertNull(sensorTypeDao.getByUid(999L))
    }

    @Test
    fun `getByDeviceId returns only sensor types for that device`() = runTest {
        val otherDeviceId = db.deviceDao().insert(DeviceEntity(uuid = "device-2", accountId = 1L, name = "D2", description = ""))
        sensorTypeDao.insert(SensorTypeEntity(uid = 1L, deviceId = deviceId, name = "Temp", description = ""))
        sensorTypeDao.insert(SensorTypeEntity(uid = 2L, deviceId = otherDeviceId, name = "Humidity", description = ""))

        val result = sensorTypeDao.getByDeviceId(deviceId)

        assertEquals(1, result.size)
        assertEquals("Temp", result.first().name)
    }

    @Test
    fun `deleting the parent device cascades and deletes its sensor types`() = runTest {
        sensorTypeDao.insert(SensorTypeEntity(uid = 1L, deviceId = deviceId, name = "Temp", description = ""))

        db.deviceDao().deleteByUuid("device-1")

        assertNull(sensorTypeDao.getByUid(1L))
    }
}
