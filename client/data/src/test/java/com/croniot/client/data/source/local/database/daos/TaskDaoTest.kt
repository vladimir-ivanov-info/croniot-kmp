package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.AccountEntity
import com.croniot.client.data.source.local.database.entities.DeviceEntity
import com.croniot.client.data.source.local.database.entities.TaskEntity
import com.croniot.client.data.source.local.database.entities.TaskTypeEntity
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
class TaskDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private var deviceId: Long = 0
    private var taskTypeId: Long = 0

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = db.taskDao()
        val accountId = db.accountDao().insert(AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com"))
        deviceId = db.deviceDao().insert(DeviceEntity(uuid = "device-1", accountId = accountId, name = "Device", description = ""))
        taskTypeId = db.taskTypeDao().insert(TaskTypeEntity(uid = 1L, deviceId = deviceId, name = "Water", description = ""))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `WHEN a task is inserted THEN getByUid returns the persisted task`() = runTest {
        taskDao.insert(TaskEntity(uid = 1L, deviceId = deviceId, taskTypeId = taskTypeId))

        val result = taskDao.getByUid(1L)

        assertEquals(1L, result?.uid)
        assertEquals(deviceId, result?.deviceId)
    }

    @Test
    fun `WHEN uid is unknown THEN getByUid returns null`() = runTest {
        assertNull(taskDao.getByUid(999L))
    }

    @Test
    fun `WHEN inserting with the same row id THEN it replaces the previous values`() = runTest {
        val rowId = taskDao.insert(TaskEntity(uid = 1L, deviceId = deviceId, taskTypeId = taskTypeId, parametersValuesJson = "{}"))
        taskDao.insert(TaskEntity(id = rowId, uid = 1L, deviceId = deviceId, taskTypeId = taskTypeId, parametersValuesJson = "{\"a\":1}"))

        val all = taskDao.getByDeviceId(deviceId)

        assertEquals(1, all.size)
        assertEquals("{\"a\":1}", all.first().parametersValuesJson)
    }

    @Test
    fun `WHEN inserting with the same uid but no explicit id THEN it creates two independent rows`() = runTest {
        taskDao.insert(TaskEntity(uid = 1L, deviceId = deviceId, taskTypeId = taskTypeId))
        taskDao.insert(TaskEntity(uid = 1L, deviceId = deviceId, taskTypeId = taskTypeId))

        val all = taskDao.getByDeviceId(deviceId)

        assertEquals(2, all.size)
    }

    @Test
    fun `WHEN tasks exist for other devices THEN getByDeviceId only returns those for that device`() = runTest {
        val otherDeviceId = db.deviceDao().insert(DeviceEntity(uuid = "device-2", accountId = 1L, name = "D2", description = ""))
        taskDao.insert(TaskEntity(uid = 1L, deviceId = deviceId, taskTypeId = taskTypeId))
        taskDao.insert(TaskEntity(uid = 2L, deviceId = otherDeviceId, taskTypeId = taskTypeId))

        val result = taskDao.getByDeviceId(deviceId)

        assertEquals(1, result.size)
        assertEquals(1L, result.first().uid)
    }

    @Test
    fun `WHEN tasks exist of other types THEN getByTaskTypeId only returns those of that type`() = runTest {
        val otherTaskTypeId = db.taskTypeDao().insert(TaskTypeEntity(uid = 2L, deviceId = deviceId, name = "Light", description = ""))
        taskDao.insert(TaskEntity(uid = 1L, deviceId = deviceId, taskTypeId = taskTypeId))
        taskDao.insert(TaskEntity(uid = 2L, deviceId = deviceId, taskTypeId = otherTaskTypeId))

        val result = taskDao.getByTaskTypeId(taskTypeId)

        assertEquals(1, result.size)
        assertEquals(1L, result.first().uid)
    }

    @Test
    fun `WHEN the parent device is deleted THEN it cascades and deletes its tasks`() = runTest {
        taskDao.insert(TaskEntity(uid = 1L, deviceId = deviceId, taskTypeId = taskTypeId))

        db.deviceDao().deleteByUuid("device-1")

        assertNull(taskDao.getByUid(1L))
    }
}
