package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.AccountEntity
import com.croniot.client.data.source.local.database.entities.DeviceEntity
import com.croniot.client.data.source.local.database.entities.ParameterTaskEntity
import com.croniot.client.data.source.local.database.entities.TaskTypeEntity
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
class ParameterTaskDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var parameterTaskDao: ParameterTaskDao
    private var taskTypeId: Long = 0

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        parameterTaskDao = db.parameterTaskDao()
        val accountId = db.accountDao().insert(AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com"))
        val deviceId = db.deviceDao().insert(DeviceEntity(uuid = "device-1", accountId = accountId, name = "Device", description = ""))
        taskTypeId = db.taskTypeDao().insert(TaskTypeEntity(uid = 1L, deviceId = deviceId, name = "Water", description = ""))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `WHEN a parameter is inserted THEN getByTaskTypeId returns the persisted parameter`() = runTest {
        parameterTaskDao.insert(
            ParameterTaskEntity(uid = 1L, taskTypeId = taskTypeId, name = "duration", type = "number", unit = "s", description = ""),
        )

        val result = parameterTaskDao.getByTaskTypeId(taskTypeId)

        assertEquals(1, result.size)
        assertEquals("duration", result.first().name)
    }

    @Test
    fun `WHEN there are no parameters THEN getByTaskTypeId returns an empty list`() = runTest {
        assertTrue(parameterTaskDao.getByTaskTypeId(taskTypeId).isEmpty())
    }

    @Test
    fun `WHEN the parent task type is deleted THEN it cascades and deletes its parameters`() = runTest {
        parameterTaskDao.insert(
            ParameterTaskEntity(uid = 1L, taskTypeId = taskTypeId, name = "duration", type = "number", unit = "s", description = ""),
        )

        db.deviceDao().deleteByUuid("device-1")

        assertTrue(parameterTaskDao.getByTaskTypeId(taskTypeId).isEmpty())
    }
}
