package com.croniot.client.data.repositories

import Outcome
import app.cash.turbine.test
import com.croniot.client.data.source.remote.mqtt.TasksDataSource
import com.croniot.client.data.source.taskhistory.LocalTaskHistoryDataSource
import com.croniot.client.data.source.transport.TransportRouter
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.models.TransportKind
import croniot.models.TaskKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

class TasksRepositoryImplTest {

    private val cloudTasksDataSource: TasksDataSource = mockk()
    private val bleTasksDataSource: TasksDataSource = mockk()
    private val transportRouter: TransportRouter = mockk()
    private val localTaskHistoryDataSource: LocalTaskHistoryDataSource = mockk()

    private lateinit var repository: TasksRepositoryImpl

    private val deviceUuid = "device-1"

    @BeforeEach
    fun setUp() {
        every { transportRouter.transportFor(deviceUuid) } returns TransportKind.CLOUD
        repository = TasksRepositoryImpl(
            cloudTasksDataSource = cloudTasksDataSource,
            bleTasksDataSource = bleTasksDataSource,
            transportRouter = transportRouter,
            localTaskHistoryDataSource = localTaskHistoryDataSource,
        )
    }

    private fun task(uid: Long, taskTypeUid: Long = 10L, stateInfo: TaskStateInfo? = null) =
        Task(deviceUuid = deviceUuid, taskTypeUid = taskTypeUid, uid = uid, initialTaskStateInfo = stateInfo)

    private fun stateInfo(state: String = "RUNNING", progress: Double = 0.5) =
        TaskStateInfo(dateTime = ZonedDateTime.now(), state = state, progress = progress, errorMessage = "")

    @Test
    fun `fetchTasks delegates to cloud data source when transport is CLOUD`() = runTest {
        val tasks = listOf(task(1L))
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        val result = repository.fetchTasks(deviceUuid)

        assertEquals(Outcome.Ok(tasks), result)
        coVerify(exactly = 1) { cloudTasksDataSource.fetchTasks(deviceUuid) }
    }

    @Test
    fun `fetchTasks delegates to ble data source when transport is BLE`() = runTest {
        every { transportRouter.transportFor(deviceUuid) } returns TransportKind.BLE
        val tasks = listOf(task(1L))
        coEvery { bleTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        val result = repository.fetchTasks(deviceUuid)

        assertEquals(Outcome.Ok(tasks), result)
        coVerify(exactly = 1) { bleTasksDataSource.fetchTasks(deviceUuid) }
        coVerify(exactly = 0) { cloudTasksDataSource.fetchTasks(any()) }
    }

    @Test
    fun `fetchTasks returns cached result on second call without hitting data source again`() = runTest {
        val tasks = listOf(task(1L), task(2L))
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        repository.fetchTasks(deviceUuid)
        val second = repository.fetchTasks(deviceUuid)

        assertEquals(Outcome.Ok(tasks), second)
        coVerify(exactly = 1) { cloudTasksDataSource.fetchTasks(deviceUuid) }
    }

    @Test
    fun `fetchTasks propagates error from data source`() = runTest {
        val error = TaskError.Remote(RemoteError.Unreachable)
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Err(error)

        val result = repository.fetchTasks(deviceUuid)

        assertEquals(Outcome.Err(error), result)
    }

    @Test
    fun `fetchTasks stores initial task state info for later retrieval`() = runTest {
        val info = stateInfo(state = "RUNNING")
        val tasks = listOf(task(uid = 1L, taskTypeUid = 10L, stateInfo = info))
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        repository.fetchTasks(deviceUuid)

        val latest = repository.getLatestTaskStateInfo(deviceUuid, taskTypeUid = 10L)
        assertEquals(info, latest)
    }

    @Test
    fun `getLatestTaskUidForTaskType returns highest uid for given task type`() = runTest {
        val tasks = listOf(task(uid = 1L, taskTypeUid = 10L), task(uid = 5L, taskTypeUid = 10L), task(uid = 3L, taskTypeUid = 20L))
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        repository.fetchTasks(deviceUuid)

        assertEquals(5L, repository.getLatestTaskUidForTaskType(deviceUuid, 10L))
    }

    @Test
    fun `getLatestTaskUidForTaskType returns null when no tasks cached`() {
        assertNull(repository.getLatestTaskUidForTaskType(deviceUuid, 10L))
    }

    @Test
    fun `getLatestTaskStateInfoEmittedByIoT excludes CREATED UNDEFINED and ERROR states`() = runTest {
        val key = TaskKey(deviceUuid = deviceUuid, taskTypeUid = 10L, taskUid = 1L)
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns
            Outcome.Ok(listOf(task(uid = 1L, taskTypeUid = 10L, stateInfo = stateInfo(state = "CREATED"))))
        repository.fetchTasks(deviceUuid)

        val result = repository.getLatestTaskStateInfoEmittedByIoT(deviceUuid, 10L)

        assertNull(result)
    }

    @Test
    fun `getLatestTaskStateInfoEmittedByIoT returns state when not excluded`() = runTest {
        val info = stateInfo(state = "RUNNING")
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns
            Outcome.Ok(listOf(task(uid = 1L, taskTypeUid = 10L, stateInfo = info)))
        repository.fetchTasks(deviceUuid)

        val result = repository.getLatestTaskStateInfoEmittedByIoT(deviceUuid, 10L)

        assertEquals(info, result)
    }

    @Test
    fun `observeNewTasks emits task pushed via listenTasks callback`() = runTest {
        val newTask = task(1L)
        coEvery { cloudTasksDataSource.listenTasks(deviceUuid, any()) } coAnswers {
            secondArg<(Task) -> Unit>().invoke(newTask)
        }

        repository.observeNewTasks(deviceUuid).test(timeout = 5.seconds) {
            repository.listenTasks(deviceUuid)
            assertEquals(newTask, awaitItem())
        }
    }

    @Test
    fun `observeTaskStateInfoUpdates emits event pushed via listenTaskStateInfos callback`() = runTest {
        val key = TaskKey(deviceUuid = deviceUuid, taskTypeUid = 10L, taskUid = 1L)
        val info = stateInfo()
        coEvery { cloudTasksDataSource.listenTaskStateInfos(deviceUuid, any()) } coAnswers {
            secondArg<(com.croniot.client.domain.models.events.TaskStateInfoEvent) -> Unit>()
                .invoke(com.croniot.client.domain.models.events.TaskStateInfoEvent(key = key, info = info))
        }

        repository.observeTaskStateInfoUpdates(deviceUuid).test(timeout = 5.seconds) {
            repository.listenTaskStateInfos(deviceUuid)
            val event = awaitItem()
            assertEquals(info, event.info)
        }
    }

    @Test
    fun `listenTasks callback stores initial task state info and emits it when the incoming task has one`() = runTest {
        val info = stateInfo(state = "RUNNING")
        val newTask = task(uid = 1L, taskTypeUid = 10L, stateInfo = info)
        coEvery { cloudTasksDataSource.listenTasks(deviceUuid, any()) } coAnswers {
            secondArg<(Task) -> Unit>().invoke(newTask)
        }

        repository.observeTaskStateInfoUpdates(deviceUuid).test(timeout = 5.seconds) {
            repository.listenTasks(deviceUuid)
            val event = awaitItem()
            assertEquals(info, event.info)
        }
        assertEquals(info, repository.getLatestTaskStateInfo(deviceUuid, taskTypeUid = 10L))
    }

    @Test
    fun `addTask does nothing when the task has no initial state info`() = runTest {
        repository.addTask(task(uid = 1L, taskTypeUid = 10L, stateInfo = null))

        assertNull(repository.getLatestTaskStateInfo(deviceUuid, taskTypeUid = 10L))
    }

    @Test
    fun `addTask stores the initial task state info and emits a TaskStateInfoEvent`() = runTest {
        val info = stateInfo(state = "RUNNING")
        val newTask = task(uid = 1L, taskTypeUid = 10L, stateInfo = info)

        repository.observeTaskStateInfoUpdates(deviceUuid).test(timeout = 5.seconds) {
            repository.addTask(newTask)
            val event = awaitItem()
            assertEquals(info, event.info)
        }
        assertEquals(info, repository.getLatestTaskStateInfo(deviceUuid, taskTypeUid = 10L))
    }

    @Test
    fun `stopListeningFor clears cached tasks and state for that device`() = runTest {
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(listOf(task(1L)))
        coEvery { cloudTasksDataSource.stopListening(deviceUuid) } returns Unit
        repository.fetchTasks(deviceUuid)

        repository.stopListeningFor(deviceUuid)

        assertNull(repository.getLatestTaskUidForTaskType(deviceUuid, 10L))
        coVerify(exactly = 1) { cloudTasksDataSource.stopListening(deviceUuid) }
    }

    @Test
    fun `stopAllListeners stops both cloud and ble data sources`() = runTest {
        coEvery { cloudTasksDataSource.stopAllListeners() } returns Unit
        coEvery { bleTasksDataSource.stopAllListeners() } returns Unit

        repository.stopAllListeners()

        coVerify(exactly = 1) { cloudTasksDataSource.stopAllListeners() }
        coVerify(exactly = 1) { bleTasksDataSource.stopAllListeners() }
    }

    @Test
    fun `sendNewTask delegates to data source for device transport`() = runTest {
        val newTask = task(1L)
        coEvery { cloudTasksDataSource.sendNewTask(any()) } returns Outcome.Ok(Unit)

        val result = repository.sendNewTask(newTask)

        assertEquals(Outcome.Ok(Unit), result)
        coVerify(exactly = 1) { cloudTasksDataSource.sendNewTask(any()) }
    }

    @Test
    fun `requestTaskStateInfoSync delegates to data source`() = runTest {
        coEvery { cloudTasksDataSource.requestTaskStateInfoSync(deviceUuid, 10L) } returns Outcome.Ok(Unit)

        val result = repository.requestTaskStateInfoSync(deviceUuid, 10L)

        assertEquals(Outcome.Ok(Unit), result)
    }

    @Test
    fun `fetchTaskStateInfoHistory returns local page immediately when it satisfies the limit`() = runTest {
        val entry = TaskStateInfoHistoryEntry(
            stateInfoId = 1L,
            taskKey = TaskKey(deviceUuid, 10L, 1L),
            stateInfo = stateInfo(),
        )
        coEvery {
            localTaskHistoryDataSource.getPage(deviceUuid, 10, null, null)
        } returns List(10) { entry }

        val result = repository.fetchTaskStateInfoHistory(deviceUuid, limit = 10, before = null, beforeId = null, taskTypeUid = null)

        assertEquals(Outcome.Ok(List(10) { entry }), result)
        coVerify(exactly = 0) { cloudTasksDataSource.fetchTaskStateInfoHistory(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `fetchTaskStateInfoHistory falls back to remote when local page is smaller than limit`() = runTest {
        val entry = TaskStateInfoHistoryEntry(
            stateInfoId = 1L,
            taskKey = TaskKey(deviceUuid, 10L, 1L),
            stateInfo = stateInfo(),
        )
        coEvery { localTaskHistoryDataSource.getPage(deviceUuid, 10, null, null) } returns listOf(entry)
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistory(deviceUuid, 10, null, null, null)
        } returns Outcome.Ok(listOf(entry, entry))
        coEvery { localTaskHistoryDataSource.savePage(deviceUuid, any()) } returns Unit

        val result = repository.fetchTaskStateInfoHistory(deviceUuid, limit = 10, before = null, beforeId = null, taskTypeUid = null)

        assertTrue(result is Outcome.Ok)
        coVerify(exactly = 1) { localTaskHistoryDataSource.savePage(deviceUuid, listOf(entry, entry)) }
    }

    @Test
    fun `fetchTaskStateInfoHistory with taskTypeUid filter skips local cache entirely`() = runTest {
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistory(deviceUuid, 10, null, null, 99L)
        } returns Outcome.Ok(emptyList())

        repository.fetchTaskStateInfoHistory(deviceUuid, limit = 10, before = null, beforeId = null, taskTypeUid = 99L)

        coVerify(exactly = 0) { localTaskHistoryDataSource.getPage(any(), any(), any(), any()) }
        coVerify(exactly = 1) { cloudTasksDataSource.fetchTaskStateInfoHistory(deviceUuid, 10, null, null, 99L) }
    }

    @Test
    fun `fetchTaskStateInfoHistory returns local page when remote call fails`() = runTest {
        val entry = TaskStateInfoHistoryEntry(
            stateInfoId = 1L,
            taskKey = TaskKey(deviceUuid, 10L, 1L),
            stateInfo = stateInfo(),
        )
        coEvery { localTaskHistoryDataSource.getPage(deviceUuid, 10, null, null) } returns listOf(entry)
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistory(deviceUuid, 10, null, null, null)
        } returns Outcome.Err(TaskError.Remote(RemoteError.Unreachable))

        val result = repository.fetchTaskStateInfoHistory(deviceUuid, limit = 10, before = null, beforeId = null, taskTypeUid = null)

        assertEquals(Outcome.Ok(listOf(entry)), result)
    }

    @Test
    fun `fetchTaskStateInfoHistory returns error when both local and remote are empty`() = runTest {
        val error = TaskError.Remote(RemoteError.Unreachable)
        coEvery { localTaskHistoryDataSource.getPage(deviceUuid, 10, null, null) } returns emptyList()
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistory(deviceUuid, 10, null, null, null)
        } returns Outcome.Err(error)

        val result = repository.fetchTaskStateInfoHistory(deviceUuid, limit = 10, before = null, beforeId = null, taskTypeUid = null)

        assertEquals(Outcome.Err(error), result)
    }

    @Test
    fun `fetchTaskStateInfoHistoryCount returns max of local and remote counts`() = runTest {
        coEvery { localTaskHistoryDataSource.count(deviceUuid, null, null) } returns 5
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistoryCount(deviceUuid, null, null, null)
        } returns Outcome.Ok(8)

        val result = repository.fetchTaskStateInfoHistoryCount(deviceUuid, before = null, beforeId = null, taskTypeUid = null)

        assertEquals(Outcome.Ok(8), result)
    }

    @Test
    fun `fetchTaskStateInfoHistoryCount falls back to local count when remote fails`() = runTest {
        coEvery { localTaskHistoryDataSource.count(deviceUuid, null, null) } returns 3
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistoryCount(deviceUuid, null, null, null)
        } returns Outcome.Err(TaskError.Remote(RemoteError.Unreachable))

        val result = repository.fetchTaskStateInfoHistoryCount(deviceUuid, before = null, beforeId = null, taskTypeUid = null)

        assertEquals(Outcome.Ok(3), result)
    }

    @Test
    fun `fetchTaskStateInfoHistoryCount with taskTypeUid filter delegates directly to remote`() = runTest {
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistoryCount(deviceUuid, null, null, 42L)
        } returns Outcome.Ok(2)

        val result = repository.fetchTaskStateInfoHistoryCount(deviceUuid, before = null, beforeId = null, taskTypeUid = 42L)

        assertEquals(Outcome.Ok(2), result)
        coVerify(exactly = 0) { localTaskHistoryDataSource.count(any(), any(), any()) }
    }
}
