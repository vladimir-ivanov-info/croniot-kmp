package com.server.croniot.data.repositories

import com.server.croniot.testsupport.fakes.FakeTaskDao
import com.server.croniot.testsupport.fakes.FakeTaskStateInfoDao
import croniot.models.Task
import croniot.models.TaskStateInfo
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

/**
 * Unit tests for [TaskRepository] using fakes (not mocks) of [com.server.croniot.data.db.daos.TaskDao]
 * and [com.server.croniot.data.db.daos.TaskStateInfoDao]. Most methods here are plain delegation to
 * one of the two DAOs; they are still covered so the wiring (which DAO, which arguments) is verified
 * against realistic fake behavior. [TaskRepository.createState] is intentionally not tested: its body
 * is an empty `TODO` stub with no observable behavior, same as [AccountRepository.getAccountOfDevice].
 */
class TaskRepositoryTest {

    private val taskDao = FakeTaskDao()
    private val taskStateInfoDao = FakeTaskStateInfoDao()
    private val repository = TaskRepository(taskDao, taskStateInfoDao)

    @Test
    fun `WHEN key is known THEN get returns the task, otherwise null`() {
        val task = Task(uid = 1L, parametersValues = emptyMap(), taskTypeUid = 10L)
        taskDao.seed("device-uuid", task)

        assertEquals(task, repository.get("device-uuid", taskTypeUid = 10L, taskUid = 1L))
        assertNull(repository.get("device-uuid", taskTypeUid = 10L, taskUid = 999L))
        assertNull(repository.get("missing-device", taskTypeUid = 10L, taskUid = 1L))
    }

    @Test
    fun `WHEN createTaskState is called THEN it inserts the state info against the given task id`() {
        val stateInfo = TaskStateInfo(
            taskUid = 1L,
            dateTime = ZonedDateTime.now(),
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        )

        repository.createTaskState(stateInfo, taskId = 42L)

        assertEquals(listOf(stateInfo to 42L), taskStateInfoDao.insertedStateInfos)
    }

    @Test
    fun `WHEN create is called by taskTypeId THEN it returns a freshly created task with the given task type uid`() {
        val task = repository.create(taskTypeId = 5L, taskTypeUid = 99L)

        assertEquals(99L, task?.taskTypeUid)
    }

    @Test
    fun `WHEN create is called by task THEN it inserts the full task`() {
        val task = Task(uid = 7L, parametersValues = emptyMap(), taskTypeUid = 10L)

        repository.create(task)

        assertEquals(listOf(task), taskDao.insertedTasks)
    }

    @Test
    fun `WHEN getAll is called for a device THEN it returns only the tasks for that device`() {
        val taskA = Task(uid = 1L, parametersValues = emptyMap(), taskTypeUid = 10L)
        val taskB = Task(uid = 2L, parametersValues = emptyMap(), taskTypeUid = 10L)
        taskDao.seed("device-1", taskA)
        taskDao.seed("device-2", taskB)

        assertEquals(listOf(taskA), repository.getAll("device-1"))
        assertTrue(repository.getAll("missing-device").isEmpty())
    }

    @Test
    fun `WHEN taskTypeUid and limit are given THEN getAllStateInfoHistory filters by task type uid and respects the limit`() {
        val entryA = historyEntry(stateInfoId = 1L, taskTypeUid = 10L)
        val entryB = historyEntry(stateInfoId = 2L, taskTypeUid = 20L)
        taskDao.seedHistory("device-uuid", entryA)
        taskDao.seedHistory("device-uuid", entryB)

        val filtered = repository.getAllStateInfoHistory("device-uuid", limit = 10, before = null, beforeId = null, taskTypeUid = 10L)
        assertEquals(listOf(entryA), filtered)

        val all = repository.getAllStateInfoHistory("device-uuid", limit = 10, before = null, beforeId = null)
        assertEquals(2, all.size)

        val limited = repository.getAllStateInfoHistory("device-uuid", limit = 1, before = null, beforeId = null)
        assertEquals(1, limited.size)
    }

    @Test
    fun `WHEN an optional task type filter is given THEN getAllStateInfoHistoryCount counts only matching entries`() {
        taskDao.seedHistory("device-uuid", historyEntry(stateInfoId = 1L, taskTypeUid = 10L))
        taskDao.seedHistory("device-uuid", historyEntry(stateInfoId = 2L, taskTypeUid = 20L))

        assertEquals(2, repository.getAllStateInfoHistoryCount("device-uuid", before = null, beforeId = null))
        assertEquals(1, repository.getAllStateInfoHistoryCount("device-uuid", before = null, beforeId = null, taskTypeUid = 10L))
        assertEquals(0, repository.getAllStateInfoHistoryCount("missing-device", before = null, beforeId = null))
    }

    private fun historyEntry(stateInfoId: Long, taskTypeUid: Long) = TaskStateInfoHistoryEntryDto(
        stateInfoId = stateInfoId,
        taskUid = 1L,
        taskTypeUid = taskTypeUid,
        dateTime = ZonedDateTime.now(),
        state = "RUNNING",
        progress = 0.0,
        errorMessage = "",
    )
}
