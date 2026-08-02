package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.AccountEntity
import com.croniot.client.data.source.local.database.entities.DeviceEntity
import com.croniot.client.data.source.local.database.entities.ParameterSensorEntity
import com.croniot.client.data.source.local.database.entities.SensorTypeEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ParameterSensorDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var parameterSensorDao: ParameterSensorDao
    private var sensorTypeId: Long = 0

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        parameterSensorDao = db.parameterSensorDao()
        val accountId = db.accountDao().insert(AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com"))
        val deviceId = db.deviceDao().insert(DeviceEntity(uuid = "device-1", accountId = accountId, name = "Device", description = ""))
        sensorTypeId = db.sensorTypeDao().insert(SensorTypeEntity(uid = 1L, deviceId = deviceId, name = "Temp", description = ""))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `WHEN a parameter is inserted THEN getBySensorTypeId returns the persisted parameter`() = runTest {
        parameterSensorDao.insert(
            ParameterSensorEntity(uid = 1L, sensorTypeId = sensorTypeId, name = "threshold", type = "number", unit = "c", description = ""),
        )

        val result = parameterSensorDao.getBySensorTypeId(sensorTypeId)

        assertEquals(1, result.size)
        assertEquals("threshold", result.first().name)
    }

    @Test
    fun `WHEN there are no parameters THEN getBySensorTypeId returns an empty list`() = runTest {
        val result = parameterSensorDao.getBySensorTypeId(sensorTypeId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `WHEN the parent sensor type is deleted THEN it cascades and deletes its parameters`() = runTest {
        parameterSensorDao.insert(
            ParameterSensorEntity(uid = 1L, sensorTypeId = sensorTypeId, name = "threshold", type = "number", unit = "c", description = ""),
        )

        db.deviceDao().deleteByUuid("device-1")

        assertTrue(parameterSensorDao.getBySensorTypeId(sensorTypeId).isEmpty())
    }
}
