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
    fun `WHEN transport is CLOUD THEN fetchTasks delegates to cloud data source`() = runTest {
        val tasks = listOf(task(1L))
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        val result = repository.fetchTasks(deviceUuid)

        assertEquals(Outcome.Ok(tasks), result)
        coVerify(exactly = 1) { cloudTasksDataSource.fetchTasks(deviceUuid) }
    }

    @Test
    fun `WHEN transport is BLE THEN fetchTasks delegates to ble data source`() = runTest {
        every { transportRouter.transportFor(deviceUuid) } returns TransportKind.BLE
        val tasks = listOf(task(1L))
        coEvery { bleTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        val result = repository.fetchTasks(deviceUuid)

        assertEquals(Outcome.Ok(tasks), result)
        coVerify(exactly = 1) { bleTasksDataSource.fetchTasks(deviceUuid) }
        coVerify(exactly = 0) { cloudTasksDataSource.fetchTasks(any()) }
    }

    @Test
    fun `WHEN fetchTasks is called a second time THEN it returns the cached result without hitting the data source again`() = runTest {
        val tasks = listOf(task(1L), task(2L))
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        repository.fetchTasks(deviceUuid)
        val second = repository.fetchTasks(deviceUuid)

        assertEquals(Outcome.Ok(tasks), second)
        coVerify(exactly = 1) { cloudTasksDataSource.fetchTasks(deviceUuid) }
    }

    @Test
    fun `WHEN data source returns an error THEN fetchTasks propagates it`() = runTest {
        val error = TaskError.Remote(RemoteError.Unreachable)
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Err(error)

        val result = repository.fetchTasks(deviceUuid)

        assertEquals(Outcome.Err(error), result)
    }

    @Test
    fun `WHEN fetchTasks is called THEN it stores initial task state info for later retrieval`() = runTest {
        val info = stateInfo(state = "RUNNING")
        val tasks = listOf(task(uid = 1L, taskTypeUid = 10L, stateInfo = info))
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        repository.fetchTasks(deviceUuid)

        val latest = repository.getLatestTaskStateInfo(deviceUuid, taskTypeUid = 10L)
        assertEquals(info, latest)
    }

    @Test
    fun `WHEN multiple tasks exist for a task type THEN getLatestTaskUidForTaskType returns the highest uid`() = runTest {
        val tasks = listOf(task(uid = 1L, taskTypeUid = 10L), task(uid = 5L, taskTypeUid = 10L), task(uid = 3L, taskTypeUid = 20L))
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(tasks)

        repository.fetchTasks(deviceUuid)

        assertEquals(5L, repository.getLatestTaskUidForTaskType(deviceUuid, 10L))
    }

    @Test
    fun `WHEN no tasks are cached THEN getLatestTaskUidForTaskType returns null`() {
        assertNull(repository.getLatestTaskUidForTaskType(deviceUuid, 10L))
    }

    @Test
    fun `WHEN latest state is CREATED THEN getLatestTaskStateInfoEmittedByIoT excludes it`() = runTest {
        val key = TaskKey(deviceUuid = deviceUuid, taskTypeUid = 10L, taskUid = 1L)
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns
            Outcome.Ok(listOf(task(uid = 1L, taskTypeUid = 10L, stateInfo = stateInfo(state = "CREATED"))))
        repository.fetchTasks(deviceUuid)

        val result = repository.getLatestTaskStateInfoEmittedByIoT(deviceUuid, 10L)

        assertNull(result)
    }

    @Test
    fun `WHEN latest state is not excluded THEN getLatestTaskStateInfoEmittedByIoT returns it`() = runTest {
        val info = stateInfo(state = "RUNNING")
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns
            Outcome.Ok(listOf(task(uid = 1L, taskTypeUid = 10L, stateInfo = info)))
        repository.fetchTasks(deviceUuid)

        val result = repository.getLatestTaskStateInfoEmittedByIoT(deviceUuid, 10L)

        assertEquals(info, result)
    }

    @Test
    fun `WHEN a task is pushed via listenTasks callback THEN observeNewTasks emits it`() = runTest {
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
    fun `WHEN an event is pushed via listenTaskStateInfos callback THEN observeTaskStateInfoUpdates emits it`() = runTest {
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
    fun `WHEN the incoming task has an initial task state info THEN listenTasks callback stores it and emits it`() = runTest {
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
    fun `WHEN the task has no initial state info THEN addTask does nothing`() = runTest {
        repository.addTask(task(uid = 1L, taskTypeUid = 10L, stateInfo = null))

        assertNull(repository.getLatestTaskStateInfo(deviceUuid, taskTypeUid = 10L))
    }

    @Test
    fun `WHEN the task has an initial state info THEN addTask stores it and emits a TaskStateInfoEvent`() = runTest {
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
    fun `WHEN stopListeningFor is called THEN it clears cached tasks and state for that device`() = runTest {
        coEvery { cloudTasksDataSource.fetchTasks(deviceUuid) } returns Outcome.Ok(listOf(task(1L)))
        coEvery { cloudTasksDataSource.stopListening(deviceUuid) } returns Unit
        repository.fetchTasks(deviceUuid)

        repository.stopListeningFor(deviceUuid)

        assertNull(repository.getLatestTaskUidForTaskType(deviceUuid, 10L))
        coVerify(exactly = 1) { cloudTasksDataSource.stopListening(deviceUuid) }
    }

    @Test
    fun `WHEN stopAllListeners is called THEN it stops both cloud and ble data sources`() = runTest {
        coEvery { cloudTasksDataSource.stopAllListeners() } returns Unit
        coEvery { bleTasksDataSource.stopAllListeners() } returns Unit

        repository.stopAllListeners()

        coVerify(exactly = 1) { cloudTasksDataSource.stopAllListeners() }
        coVerify(exactly = 1) { bleTasksDataSource.stopAllListeners() }
    }

    @Test
    fun `WHEN sendNewTask is called THEN it delegates to the data source for the device transport`() = runTest {
        val newTask = task(1L)
        coEvery { cloudTasksDataSource.sendNewTask(any()) } returns Outcome.Ok(Unit)

        val result = repository.sendNewTask(newTask)

        assertEquals(Outcome.Ok(Unit), result)
        coVerify(exactly = 1) { cloudTasksDataSource.sendNewTask(any()) }
    }

    @Test
    fun `WHEN requestTaskStateInfoSync is called THEN it delegates to data source`() = runTest {
        coEvery { cloudTasksDataSource.requestTaskStateInfoSync(deviceUuid, 10L) } returns Outcome.Ok(Unit)

        val result = repository.requestTaskStateInfoSync(deviceUuid, 10L)

        assertEquals(Outcome.Ok(Unit), result)
    }

    @Test
    fun `WHEN local page satisfies the limit THEN fetchTaskStateInfoHistory returns it immediately`() = runTest {
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
    fun `WHEN local page is smaller than the limit THEN fetchTaskStateInfoHistory falls back to remote`() = runTest {
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
    fun `WHEN a taskTypeUid filter is given THEN fetchTaskStateInfoHistory skips local cache entirely`() = runTest {
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistory(deviceUuid, 10, null, null, 99L)
        } returns Outcome.Ok(emptyList())

        repository.fetchTaskStateInfoHistory(deviceUuid, limit = 10, before = null, beforeId = null, taskTypeUid = 99L)

        coVerify(exactly = 0) { localTaskHistoryDataSource.getPage(any(), any(), any(), any()) }
        coVerify(exactly = 1) { cloudTasksDataSource.fetchTaskStateInfoHistory(deviceUuid, 10, null, null, 99L) }
    }

    @Test
    fun `WHEN remote call fails THEN fetchTaskStateInfoHistory returns the local page`() = runTest {
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
    fun `WHEN both local and remote are empty THEN fetchTaskStateInfoHistory returns an error`() = runTest {
        val error = TaskError.Remote(RemoteError.Unreachable)
        coEvery { localTaskHistoryDataSource.getPage(deviceUuid, 10, null, null) } returns emptyList()
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistory(deviceUuid, 10, null, null, null)
        } returns Outcome.Err(error)

        val result = repository.fetchTaskStateInfoHistory(deviceUuid, limit = 10, before = null, beforeId = null, taskTypeUid = null)

        assertEquals(Outcome.Err(error), result)
    }

    @Test
    fun `WHEN local and remote counts differ THEN fetchTaskStateInfoHistoryCount returns the max of both`() = runTest {
        coEvery { localTaskHistoryDataSource.count(deviceUuid, null, null) } returns 5
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistoryCount(deviceUuid, null, null, null)
        } returns Outcome.Ok(8)

        val result = repository.fetchTaskStateInfoHistoryCount(deviceUuid, before = null, beforeId = null, taskTypeUid = null)

        assertEquals(Outcome.Ok(8), result)
    }

    @Test
    fun `WHEN remote fails THEN fetchTaskStateInfoHistoryCount falls back to local count`() = runTest {
        coEvery { localTaskHistoryDataSource.count(deviceUuid, null, null) } returns 3
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistoryCount(deviceUuid, null, null, null)
        } returns Outcome.Err(TaskError.Remote(RemoteError.Unreachable))

        val result = repository.fetchTaskStateInfoHistoryCount(deviceUuid, before = null, beforeId = null, taskTypeUid = null)

        assertEquals(Outcome.Ok(3), result)
    }

    @Test
    fun `WHEN a taskTypeUid filter is given THEN fetchTaskStateInfoHistoryCount delegates directly to remote`() = runTest {
        coEvery {
            cloudTasksDataSource.fetchTaskStateInfoHistoryCount(deviceUuid, null, null, 42L)
        } returns Outcome.Ok(2)

        val result = repository.fetchTaskStateInfoHistoryCount(deviceUuid, before = null, beforeId = null, taskTypeUid = 42L)

        assertEquals(Outcome.Ok(2), result)
        coVerify(exactly = 0) { localTaskHistoryDataSource.count(any(), any(), any()) }
    }
}
