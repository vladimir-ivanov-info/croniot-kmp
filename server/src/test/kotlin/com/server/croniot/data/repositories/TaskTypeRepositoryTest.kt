package com.server.croniot.data.repositories

import com.server.croniot.testsupport.fakes.FakeParameterTaskDao
import com.server.croniot.testsupport.fakes.FakeTaskTypeDao
import croniot.models.Device
import croniot.models.ParameterTask
import croniot.models.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [TaskTypeRepository] using fakes (not mocks) of
 * [com.server.croniot.data.db.daos.TaskTypeDao] and [com.server.croniot.data.db.daos.ParameterTaskDao].
 */
class TaskTypeRepositoryTest {

    private val taskTypeDao = FakeTaskTypeDao()
    private val parameterTaskDao = FakeParameterTaskDao()
    private val repository = TaskTypeRepository(taskTypeDao, parameterTaskDao)

    private val device = Device(uuid = "device-uuid", name = "Device", iot = true)

    @Test
    fun `WHEN device and task type are known THEN getId returns non-null, otherwise null`() {
        taskTypeDao.seed(deviceId = 1L, taskType = TaskType(uid = 10L, name = "Water", description = ""))

        assertEquals(true, repository.getId(deviceId = 1L, taskTypeUid = 10L) != null)
        assertNull(repository.getId(deviceId = 1L, taskTypeUid = 999L))
    }

    @Test
    fun `WHEN device and uid are known THEN get returns the task type, otherwise null`() {
        val taskType = TaskType(uid = 10L, name = "Water", description = "")
        taskTypeDao.seedForDevice(device, taskType)

        assertEquals(taskType, repository.get(device, taskTypeUid = 10L))
        assertNull(repository.get(device, taskTypeUid = 999L))
    }

    @Test
    fun `WHEN device and uid are known THEN getLazy returns the task type, otherwise null`() {
        val taskType = TaskType(uid = 10L, name = "Water", description = "")
        taskTypeDao.seedForDevice(device, taskType)

        assertEquals(taskType, repository.getLazy(device, taskTypeUid = 10L))
        assertNull(repository.getLazy(device, taskTypeUid = 999L))
    }

    @Test
    fun `WHEN task type is seeded for that device THEN exists is true, otherwise false`() {
        taskTypeDao.seed(deviceId = 1L, taskType = TaskType(uid = 10L, name = "Water", description = ""))

        assertTrue(repository.exists(taskTypeUid = 10L, deviceId = 1L))
        assertFalse(repository.exists(taskTypeUid = 10L, deviceId = 2L))
        assertFalse(repository.exists(taskTypeUid = 999L, deviceId = 1L))
    }

    @Test
    fun `WHEN insert is called THEN the task type is persisted and becomes visible to exists`() {
        val taskType = TaskType(uid = 10L, name = "Water", description = "")

        repository.insert(taskType, deviceId = 1L)

        assertTrue(repository.exists(taskTypeUid = 10L, deviceId = 1L))
    }

    @Test
    fun `WHEN task type is known THEN getParameterTaskByUid returns the parameter, otherwise null`() {
        val parameter = ParameterTask(uid = 5L, name = "duration", type = "int", unit = "s", description = "")
        parameterTaskDao.seed(taskTypeId = 1L, parameterTask = parameter)

        assertEquals(parameter, repository.getParameterTaskByUid(parameterUid = 5L, taskTypeId = 1L))
        assertNull(repository.getParameterTaskByUid(parameterUid = 999L, taskTypeId = 1L))
    }
}
