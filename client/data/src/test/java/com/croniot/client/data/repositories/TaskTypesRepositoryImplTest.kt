package com.croniot.client.data.repositories

import com.croniot.client.core.models.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TaskTypesRepositoryImplTest {

    private lateinit var repository: TaskTypesRepositoryImpl

    private val deviceUuid = "device-uuid-123"
    private val taskType = TaskType(uid = 42L, name = "Water", description = "Water plants", parameters = emptyList())

    @BeforeEach
    fun setUp() {
        repository = TaskTypesRepositoryImpl()
    }

    @Test
    fun `get returns null when cache is empty`() {
        assertNull(repository.get(deviceUuid, taskType.uid))
    }

    @Test
    fun `add and get returns stored task type`() {
        repository.add(deviceUuid, taskType)
        assertEquals(taskType, repository.get(deviceUuid, taskType.uid))
    }

    @Test
    fun `get with wrong deviceUuid returns null`() {
        repository.add(deviceUuid, taskType)
        assertNull(repository.get("other-device", taskType.uid))
    }

    @Test
    fun `get with wrong uid returns null`() {
        repository.add(deviceUuid, taskType)
        assertNull(repository.get(deviceUuid, 999L))
    }

    @Test
    fun `add is idempotent - second add does not overwrite first`() {
        val original = taskType
        val duplicate = taskType.copy(name = "Modified")

        repository.add(deviceUuid, original)
        repository.add(deviceUuid, duplicate)

        assertEquals(original, repository.get(deviceUuid, taskType.uid))
    }

    @Test
    fun `different devices with same uid are stored independently`() {
        val taskTypeA = TaskType(uid = 1L, name = "A", description = "", parameters = emptyList())
        val taskTypeB = TaskType(uid = 1L, name = "B", description = "", parameters = emptyList())

        repository.add("device-A", taskTypeA)
        repository.add("device-B", taskTypeB)

        assertEquals(taskTypeA, repository.get("device-A", 1L))
        assertEquals(taskTypeB, repository.get("device-B", 1L))
    }

    @Test
    fun `same device with different uids stores both`() {
        val first = TaskType(uid = 1L, name = "First", description = "", parameters = emptyList())
        val second = TaskType(uid = 2L, name = "Second", description = "", parameters = emptyList())

        repository.add(deviceUuid, first)
        repository.add(deviceUuid, second)

        assertEquals(first, repository.get(deviceUuid, 1L))
        assertEquals(second, repository.get(deviceUuid, 2L))
    }
}
