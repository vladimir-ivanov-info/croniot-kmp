package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.AccountEntity
import com.croniot.client.data.source.local.database.entities.DeviceEntity
import com.croniot.client.data.source.local.database.entities.TaskEntity
import com.croniot.client.data.source.local.database.entities.TaskStateInfoEntity
import com.croniot.client.data.source.local.database.entities.TaskTypeEntity
import kotlinx.coroutines.flow.first
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
class TaskStateInfoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var taskStateInfoDao: TaskStateInfoDao
    private var taskId: Long = 0

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskStateInfoDao = db.taskStateInfoDao()
        val accountId = db.accountDao().insert(AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com"))
        val deviceId = db.deviceDao().insert(DeviceEntity(uuid = "device-1", accountId = accountId, name = "Device", description = ""))
        val taskTypeId = db.taskTypeDao().insert(TaskTypeEntity(uid = 1L, deviceId = deviceId, name = "Water", description = ""))
        taskId = db.taskDao().insert(TaskEntity(uid = 1L, deviceId = deviceId, taskTypeId = taskTypeId))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getByTaskId returns entries ordered by timestamp descending`() = runTest {
        taskStateInfoDao.insert(TaskStateInfoEntity(taskId = taskId, state = "CREATED", progress = 0.0, errorMessage = "", timeStampMillis = 1000L))
        taskStateInfoDao.insert(TaskStateInfoEntity(taskId = taskId, state = "RUNNING", progress = 50.0, errorMessage = "", timeStampMillis = 3000L))
        taskStateInfoDao.insert(TaskStateInfoEntity(taskId = taskId, state = "COMPLETED", progress = 100.0, errorMessage = "", timeStampMillis = 2000L))

        val result = taskStateInfoDao.getByTaskId(taskId)

        assertEquals(listOf("RUNNING", "COMPLETED", "CREATED"), result.map { it.state })
    }

    @Test
    fun `getByTaskId returns an empty list when there is no history`() = runTest {
        assertEquals(emptyList<TaskStateInfoEntity>(), taskStateInfoDao.getByTaskId(taskId))
    }

    @Test
    fun `observeLatestByTaskId emits the most recent state info`() = runTest {
        taskStateInfoDao.insert(TaskStateInfoEntity(taskId = taskId, state = "CREATED", progress = 0.0, errorMessage = "", timeStampMillis = 1000L))
        taskStateInfoDao.insert(TaskStateInfoEntity(taskId = taskId, state = "RUNNING", progress = 50.0, errorMessage = "", timeStampMillis = 2000L))

        val latest = taskStateInfoDao.observeLatestByTaskId(taskId).first()

        assertEquals("RUNNING", latest?.state)
    }

    @Test
    fun `observeLatestByTaskId emits null when there is no history`() = runTest {
        assertNull(taskStateInfoDao.observeLatestByTaskId(taskId).first())
    }

    @Test
    fun `deleting the parent task cascades and deletes its state info history`() = runTest {
        taskStateInfoDao.insert(TaskStateInfoEntity(taskId = taskId, state = "CREATED", progress = 0.0, errorMessage = "", timeStampMillis = 1000L))

        db.deviceDao().deleteByUuid("device-1")

        assertEquals(emptyList<TaskStateInfoEntity>(), taskStateInfoDao.getByTaskId(taskId))
    }
}
