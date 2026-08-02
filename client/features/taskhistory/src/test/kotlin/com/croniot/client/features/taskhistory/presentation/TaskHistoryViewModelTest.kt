package com.croniot.client.features.taskhistory.presentation

import Outcome
import android.util.Log
import androidx.paging.testing.asSnapshot
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryCountUseCase
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase
import croniot.models.TaskKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fetchTaskStateInfoHistoryUseCase: FetchTaskStateInfoHistoryUseCase
    private lateinit var fetchTaskStateInfoHistoryCountUseCase: FetchTaskStateInfoHistoryCountUseCase
    private lateinit var tasksRepository: TasksRepository
    private lateinit var taskTypesRepository: TaskTypesRepository

    private lateinit var viewModel: TaskHistoryViewModel

    private val taskTypeA = TaskType(uid = 1L, name = "Watering", description = "", parameters = emptyList())
    private val taskTypeB = TaskType(uid = 2L, name = "Lighting", description = "", parameters = emptyList())

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fetchTaskStateInfoHistoryUseCase = mockk()
        fetchTaskStateInfoHistoryCountUseCase = mockk()
        tasksRepository = mockk()
        taskTypesRepository = mockk()

        every { tasksRepository.observeTaskStateInfoUpdates(any()) } returns emptyFlow()
        coEvery {
            fetchTaskStateInfoHistoryCountUseCase(any(), any(), any(), any())
        } returns Outcome.Ok(0)
        every { taskTypesRepository.getAll(any()) } returns emptyList()

        viewModel = TaskHistoryViewModel(
            fetchTaskStateInfoHistoryUseCase = fetchTaskStateInfoHistoryUseCase,
            fetchTaskStateInfoHistoryCountUseCase = fetchTaskStateInfoHistoryCountUseCase,
            tasksRepository = tasksRepository,
            taskTypesRepository = taskTypesRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `WHEN initialize is called with a new deviceUuid THEN it loads availableTaskTypes`() = runTest {
        every { taskTypesRepository.getAll("device-1") } returns listOf(taskTypeA, taskTypeB)

        viewModel.initialize("device-1")

        assertEquals(listOf(taskTypeA, taskTypeB), viewModel.availableTaskTypes.value)
    }

    @Test
    fun `WHEN initialize is called twice with the same deviceUuid THEN it does not reload availableTaskTypes`() = runTest {
        every { taskTypesRepository.getAll("device-1") } returns listOf(taskTypeA)

        viewModel.initialize("device-1")
        viewModel.initialize("device-1")

        verify(exactly = 1) { taskTypesRepository.getAll("device-1") }
    }

    @Test
    fun `WHEN initialize is called with a different deviceUuid THEN it reloads availableTaskTypes and resets the new items state`() = runTest {
        every { taskTypesRepository.getAll("device-1") } returns listOf(taskTypeA)
        every { taskTypesRepository.getAll("device-2") } returns listOf(taskTypeB)

        viewModel.initialize("device-1")
        viewModel.initialize("device-2")

        verify(exactly = 1) { taskTypesRepository.getAll("device-1") }
        verify(exactly = 1) { taskTypesRepository.getAll("device-2") }
        assertEquals(listOf(taskTypeB), viewModel.availableTaskTypes.value)
        assertTrue(viewModel.newItems.value.isEmpty())
        assertEquals(0, viewModel.newEntriesSinceSnapshot.value)
    }

    @Test
    fun `WHEN setFilter is called THEN taskTypeFilter is updated`() = runTest {
        viewModel.initialize("device-1")

        viewModel.setFilter(taskTypeA)

        assertEquals(taskTypeA, viewModel.taskTypeFilter.value)
    }

    @Test
    fun `WHEN setFilter is called THEN newItems and newEntriesSinceSnapshot are reset`() = runTest {
        viewModel.initialize("device-1")

        viewModel.setFilter(taskTypeA)

        assertTrue(viewModel.newItems.value.isEmpty())
        assertEquals(0, viewModel.newEntriesSinceSnapshot.value)
    }

    @Test
    fun `WHEN initialize completes THEN totalEntries reflects the fetchTaskStateInfoHistoryCountUseCase result`() = runTest {
        coEvery {
            fetchTaskStateInfoHistoryCountUseCase(any(), any(), any(), any())
        } returns Outcome.Ok(42)

        viewModel.initialize("device-1")
        testScheduler.advanceUntilIdle()

        assertEquals(42, viewModel.totalEntries.value)
    }

    private fun stateInfoEvent(taskTypeUid: Long = 1L, taskUid: Long = 1L, state: String = "RUNNING") =
        TaskStateInfoEvent(
            key = TaskKey(deviceUuid = "device-1", taskTypeUid = taskTypeUid, taskUid = taskUid),
            info = TaskStateInfo(dateTime = ZonedDateTime.now(), state = state, progress = 0.5, errorMessage = ""),
        )

    // A hot MutableSharedFlow lets the test emit events *after* initialize() has already run and settled,
    // matching how real events arrive asynchronously over the wire. A finite flowOf(...) would instead be
    // drained synchronously inside initialize() itself (under UnconfinedTestDispatcher), before initialize's
    // own resetLiveNewItemsState() call executes and wipes out whatever was just collected.
    private fun liveEventsFlow() = kotlinx.coroutines.flow.MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 16)

    @Test
    fun `WHEN a live task state event arrives THEN it adds a new item and increments newEntriesSinceSnapshot`() = runTest {
        val event = stateInfoEvent()
        val events = liveEventsFlow()
        every { tasksRepository.observeTaskStateInfoUpdates(any()) } returns events
        every { taskTypesRepository.get("device-1", 1L) } returns taskTypeA

        viewModel.initialize("device-1")
        events.tryEmit(event)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.newItems.value.size)
        assertEquals(1, viewModel.newEntriesSinceSnapshot.value)
        assertEquals(taskTypeA.name, viewModel.newItems.value.first().taskTypeName)
    }

    @Test
    fun `WHEN a live task state event arrives for an unregistered task type THEN it uses Unknown as the type name`() = runTest {
        val event = stateInfoEvent()
        val events = liveEventsFlow()
        every { tasksRepository.observeTaskStateInfoUpdates(any()) } returns events
        every { taskTypesRepository.get("device-1", 1L) } returns null

        viewModel.initialize("device-1")
        events.tryEmit(event)
        testScheduler.advanceUntilIdle()

        assertEquals("Unknown", viewModel.newItems.value.first().taskTypeName)
    }

    @Test
    fun `WHEN a live task state event does not match the active filter THEN it is ignored`() = runTest {
        val event = stateInfoEvent(taskTypeUid = 2L)
        val events = liveEventsFlow()
        every { tasksRepository.observeTaskStateInfoUpdates(any()) } returns events
        every { taskTypesRepository.get(any(), any()) } returns taskTypeA

        viewModel.initialize("device-1")
        viewModel.setFilter(taskTypeA)
        events.tryEmit(event)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.newItems.value.isEmpty())
    }

    @Test
    fun `WHEN a duplicate live task state event with the same identity arrives THEN it is not added twice`() = runTest {
        val event = stateInfoEvent()
        val events = liveEventsFlow()
        every { tasksRepository.observeTaskStateInfoUpdates(any()) } returns events
        every { taskTypesRepository.get("device-1", 1L) } returns taskTypeA

        viewModel.initialize("device-1")
        events.tryEmit(event)
        events.tryEmit(event)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.newItems.value.size)
        assertEquals(1, viewModel.newEntriesSinceSnapshot.value)
    }

    @Test
    fun `WHEN multiple distinct live task state events arrive THEN they are prepended newest first`() = runTest {
        val firstEvent = stateInfoEvent(taskUid = 1L)
        val secondEvent = stateInfoEvent(taskUid = 2L)
        val events = liveEventsFlow()
        every { tasksRepository.observeTaskStateInfoUpdates(any()) } returns events
        every { taskTypesRepository.get("device-1", 1L) } returns taskTypeA

        viewModel.initialize("device-1")
        events.tryEmit(firstEvent)
        events.tryEmit(secondEvent)
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.newItems.value.size)
        assertEquals(2L, viewModel.newItems.value.first().taskUid)
        assertEquals(1L, viewModel.newItems.value.last().taskUid)
    }

    @Test
    fun `WHEN the seen identities set exceeds its max size THEN the oldest identity is evicted and can be re-registered`() = runTest {
        val events = liveEventsFlow()
        every { tasksRepository.observeTaskStateInfoUpdates(any()) } returns events
        every { taskTypesRepository.get("device-1", any()) } returns taskTypeA

        viewModel.initialize("device-1")

        // MAX_SEEN_NEW_IDENTITIES is a private constant (5_000) in TaskHistoryViewModel.kt: once the
        // seen-identities set exceeds it, the oldest identity is evicted so it can be treated as new
        // again if it reappears (guards against unbounded memory growth for a long-lived live session).
        val firstEvent = stateInfoEvent(taskUid = 0L)
        events.tryEmit(firstEvent)
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.newEntriesSinceSnapshot.value)

        for (i in 1..5_000) {
            events.tryEmit(stateInfoEvent(taskUid = i.toLong()))
        }
        testScheduler.advanceUntilIdle()
        assertEquals(5_001, viewModel.newEntriesSinceSnapshot.value)

        // firstEvent's identity was evicted as the oldest once the set exceeded 5_000 entries, so
        // re-emitting the exact same event must be registered as new again.
        events.tryEmit(firstEvent)
        testScheduler.advanceUntilIdle()

        assertEquals(5_002, viewModel.newEntriesSinceSnapshot.value)
    }

    @Test
    fun `WHEN the count usecase returns an error THEN totalEntries keeps its last known value`() = runTest {
        coEvery {
            fetchTaskStateInfoHistoryCountUseCase(any(), any(), any(), any())
        } returns Outcome.Ok(10)
        viewModel.initialize("device-1")
        testScheduler.advanceUntilIdle()
        assertEquals(10, viewModel.totalEntries.value)

        coEvery {
            fetchTaskStateInfoHistoryCountUseCase(any(), any(), any(), any())
        } returns Outcome.Err(com.croniot.client.domain.errors.TaskError.Remote(com.croniot.client.domain.errors.RemoteError.Unreachable))
        viewModel.setFilter(taskTypeA)
        testScheduler.advanceUntilIdle()

        assertEquals(10, viewModel.totalEntries.value)
    }

    @Test
    fun `WHEN initialize completes THEN pagingFlow loads a page through the real Pager`() = runTest {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        val entry = TaskStateInfoHistoryEntry(
            stateInfoId = 1L,
            taskKey = TaskKey(deviceUuid = "device-1", taskTypeUid = 10L, taskUid = 1L),
            stateInfo = TaskStateInfo(
                dateTime = ZonedDateTime.now(),
                state = "RUNNING",
                progress = 0.5,
                errorMessage = "",
            ),
        )
        coEvery {
            fetchTaskStateInfoHistoryUseCase("device-1", any(), any(), any(), any())
        } returns Outcome.Ok(listOf(entry))
        every { taskTypesRepository.get("device-1", 10L) } returns taskTypeA

        viewModel.initialize("device-1")

        val snapshot = viewModel.pagingFlow.asSnapshot()

        assertEquals(1, snapshot.size)
        assertEquals(1L, snapshot.first().stateInfoId)
        assertEquals(taskTypeA.name, snapshot.first().taskTypeName)
    }
}
