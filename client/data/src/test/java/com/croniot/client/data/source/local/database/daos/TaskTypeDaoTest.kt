package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.AccountEntity
import com.croniot.client.data.source.local.database.entities.DeviceEntity
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
class TaskTypeDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var taskTypeDao: TaskTypeDao
    private var deviceId: Long = 0

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskTypeDao = db.taskTypeDao()
        val accountId = db.accountDao().insert(AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com"))
        deviceId = db.deviceDao().insert(DeviceEntity(uuid = "device-1", accountId = accountId, name = "Device", description = ""))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert then getByUid returns the persisted task type`() = runTest {
        taskTypeDao.insert(TaskTypeEntity(uid = 1L, deviceId = deviceId, name = "Water", description = "desc"))

        val result = taskTypeDao.getByUid(1L)

        assertEquals("Water", result?.name)
    }

    @Test
    fun `getByUid returns null for an unknown uid`() = runTest {
        assertNull(taskTypeDao.getByUid(999L))
    }

    @Test
    fun `getByDeviceId returns only task types for that device`() = runTest {
        val otherDeviceId = db.deviceDao().insert(DeviceEntity(uuid = "device-2", accountId = 1L, name = "D2", description = ""))
        taskTypeDao.insert(TaskTypeEntity(uid = 1L, deviceId = deviceId, name = "Water", description = ""))
        taskTypeDao.insert(TaskTypeEntity(uid = 2L, deviceId = otherDeviceId, name = "Light", description = ""))

        val result = taskTypeDao.getByDeviceId(deviceId)

        assertEquals(1, result.size)
        assertEquals("Water", result.first().name)
    }

    @Test
    fun `realTime defaults to false when not specified`() = runTest {
        taskTypeDao.insert(TaskTypeEntity(uid = 1L, deviceId = deviceId, name = "Water", description = ""))

        val result = taskTypeDao.getByUid(1L)

        assertEquals(false, result?.realTime)
    }

    @Test
    fun `deleting the parent device cascades and deletes its task types`() = runTest {
        taskTypeDao.insert(TaskTypeEntity(uid = 1L, deviceId = deviceId, name = "Water", description = ""))

        db.deviceDao().deleteByUuid("device-1")

        assertNull(taskTypeDao.getByUid(1L))
    }
}
