package com.croniot.client.features.tasktypes.presentation.create_task

import Outcome
import android.util.Log
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.usecases.GetDeviceUseCase
import com.croniot.client.domain.usecases.GetLatestTaskStateInfoUseCase
import com.croniot.client.domain.usecases.ObserveTaskStateInfoUseCase
import com.croniot.client.domain.usecases.SendNewTaskUseCase
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.TaskStateInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTaskViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val getDeviceUseCase: GetDeviceUseCase = mockk()
    private val sendNewTaskUseCase: SendNewTaskUseCase = mockk()
    private val observeTaskStateInfoUseCase: ObserveTaskStateInfoUseCase = mockk()
    private val getLatestTaskStateInfoUseCase: GetLatestTaskStateInfoUseCase = mockk()

    private lateinit var viewModel: CreateTaskViewModel

    private val deviceUuid = "device-1"
    private val taskTypeOne = TaskType(uid = 1L, name = "Task 1", description = "desc-1", parameters = emptyList())
    private val taskTypeTwo = TaskType(uid = 2L, name = "Task 2", description = "desc-2", parameters = emptyList())
    private val device = Device(
        uuid = deviceUuid,
        name = "Device 1",
        description = "desc",
        taskTypes = listOf(taskTypeOne, taskTypeTwo),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CreateTaskViewModel(
            getDeviceUseCase = getDeviceUseCase,
            sendNewTaskUseCase = sendNewTaskUseCase,
            observeTaskStateInfoUseCase = observeTaskStateInfoUseCase,
            getLatestTaskStateInfoUseCase = getLatestTaskStateInfoUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `WHEN initialize is called and the device exists THEN taskType is set to the one matching the given uid`() = runTest(testDispatcher) {
        coEvery { getDeviceUseCase(deviceUuid) } returns device

        viewModel.initialize(deviceUuid, taskTypeTwo.uid)

        assertEquals(taskTypeTwo, viewModel.taskType.value)
    }

    @Test
    fun `WHEN initialize is called and the device is not found THEN the state does not change`() = runTest(testDispatcher) {
        coEvery { getDeviceUseCase(deviceUuid) } returns null

        viewModel.initialize(deviceUuid, taskTypeOne.uid)

        assertNull(viewModel.taskType.value)
    }

    @Test
    fun `WHEN initialize is called twice THEN getDeviceUseCase is invoked only once`() = runTest(testDispatcher) {
        coEvery { getDeviceUseCase(deviceUuid) } returns device

        viewModel.initialize(deviceUuid, taskTypeOne.uid)
        viewModel.initialize(deviceUuid, taskTypeOne.uid)

        coVerify(exactly = 1) { getDeviceUseCase(deviceUuid) }
    }

    @Test
    fun `WHEN updateParameter is called multiple times and sendTask is called THEN the accumulated values are forwarded to sendNewTaskUseCase`() = runTest(testDispatcher) {
        coEvery { getDeviceUseCase(deviceUuid) } returns device
        coEvery { sendNewTaskUseCase(any(), any(), any()) } returns Outcome.Ok(Unit)
        viewModel.initialize(deviceUuid, taskTypeOne.uid)

        viewModel.updateParameter(10L, "value-10")
        viewModel.updateParameter(20L, "value-20")
        viewModel.sendTask()

        coVerify(exactly = 1) {
            sendNewTaskUseCase(
                deviceUuid,
                taskTypeOne.uid,
                mapOf(10L to "value-10", 20L to "value-20"),
            )
        }
    }

    @Test
    fun `WHEN sendTask is called without an initialized device and task type THEN sendNewTaskUseCase is not invoked`() = runTest(testDispatcher) {
        viewModel.sendTask()

        coVerify(exactly = 0) { sendNewTaskUseCase(any(), any(), any()) }
    }

    @Test
    fun `WHEN observeTaskTypeLatestState is called repeatedly THEN it returns the same StateFlow instance`() = runTest(testDispatcher) {
        val initialState = TaskStateInfo(dateTime = ZonedDateTime.now(), state = "RUNNING", progress = 0.0, errorMessage = "")
        coEvery { getLatestTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns initialState
        every { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns flowOf()

        val first = viewModel.observeTaskTypeLatestState(deviceUuid, taskTypeOne)
        val second = viewModel.observeTaskTypeLatestState(deviceUuid, taskTypeOne)

        assertSame(first, second)
        assertEquals(initialState, first.value)
    }

    @Test
    fun `WHEN a genuine IoT state arrives after a pending send THEN observeTaskTypeLatestState logs the round-trip once and clears the pending timestamp`() =
        runTest(testDispatcher) {
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            val initialState = TaskStateInfo(dateTime = ZonedDateTime.now(), state = "CREATED", progress = 0.0, errorMessage = "")
            val stateUpdates = MutableSharedFlow<TaskStateInfo>(extraBufferCapacity = 4)
            coEvery { getLatestTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns initialState
            every { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns stateUpdates
            coEvery { sendNewTaskUseCase(any(), any(), any()) } returns Outcome.Ok(Unit)

            // A pending send is required for the round-trip log to fire (sendTimestampMs > 0).
            viewModel.sendStatefulTask(deviceUuid, taskTypeOne.uid, parameterUid = 1L, newValue = "on")
            // stateIn uses WhileSubscribed, so the upstream flow is only collected while there is an
            // active subscriber to the resulting StateFlow.
            val job = launch { viewModel.observeTaskTypeLatestState(deviceUuid, taskTypeOne).collect {} }

            stateUpdates.tryEmit(initialState.copy(state = "RUNNING"))

            verify(exactly = 1) { Log.d("RTT", any()) }

            // The timestamp was cleared after the first genuine IoT state, so a second one must not log again.
            stateUpdates.tryEmit(initialState.copy(state = "RUNNING"))

            verify(exactly = 1) { Log.d("RTT", any()) }
            job.cancel()
        }

    @Test
    fun `WHEN the incoming state is CREATED, UNDEFINED, or ERROR THEN observeTaskTypeLatestState does not log a round-trip`() =
        runTest(testDispatcher) {
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            val initialState = TaskStateInfo(dateTime = ZonedDateTime.now(), state = "CREATED", progress = 0.0, errorMessage = "")
            val stateUpdates = MutableSharedFlow<TaskStateInfo>(extraBufferCapacity = 4)
            coEvery { getLatestTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns initialState
            every { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns stateUpdates
            coEvery { sendNewTaskUseCase(any(), any(), any()) } returns Outcome.Ok(Unit)

            viewModel.sendStatefulTask(deviceUuid, taskTypeOne.uid, parameterUid = 1L, newValue = "on")
            val job = launch { viewModel.observeTaskTypeLatestState(deviceUuid, taskTypeOne).collect {} }

            stateUpdates.tryEmit(initialState.copy(state = "CREATED"))
            stateUpdates.tryEmit(initialState.copy(state = "UNDEFINED"))
            stateUpdates.tryEmit(initialState.copy(state = "ERROR"))

            verify(exactly = 0) { Log.d(any(), any()) }
            job.cancel()
        }

    @Test
    fun `WHEN there is no pending send THEN observeTaskTypeLatestState does not log a round-trip`() =
        runTest(testDispatcher) {
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            val initialState = TaskStateInfo(dateTime = ZonedDateTime.now(), state = "CREATED", progress = 0.0, errorMessage = "")
            val stateUpdates = MutableSharedFlow<TaskStateInfo>(extraBufferCapacity = 4)
            coEvery { getLatestTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns initialState
            every { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns stateUpdates

            val job = launch { viewModel.observeTaskTypeLatestState(deviceUuid, taskTypeOne).collect {} }

            stateUpdates.tryEmit(initialState.copy(state = "RUNNING"))

            verify(exactly = 0) { Log.d(any(), any()) }
            job.cancel()
        }

    @Test
    fun `WHEN sendStatefulTask succeeds THEN no snackbar event is emitted`() = runTest(testDispatcher) {
        coEvery { sendNewTaskUseCase(deviceUuid, taskTypeOne.uid, any()) } returns Outcome.Ok(Unit)
        val events = mutableListOf<CreateTaskUiEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.sendStatefulTask(deviceUuid, taskTypeOne.uid, parameterUid = 5L, newValue = "on")

        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test
    fun `WHEN sendStatefulTask fails THEN a ShowSnackbar event is emitted`() = runTest(testDispatcher) {
        coEvery { sendNewTaskUseCase(deviceUuid, taskTypeOne.uid, any()) } returns
            Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
        val events = mutableListOf<CreateTaskUiEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.sendStatefulTask(deviceUuid, taskTypeOne.uid, parameterUid = 5L, newValue = "on")

        assertEquals(1, events.size)
        assertTrue(events.first() is CreateTaskUiEvent.ShowSnackbar)
        job.cancel()
    }

    @Test
    fun `WHEN sendStatefulTask is called THEN it sends only the single updated parameter`() = runTest(testDispatcher) {
        coEvery { sendNewTaskUseCase(any(), any(), any()) } returns Outcome.Ok(Unit)

        viewModel.sendStatefulTask(deviceUuid, taskTypeOne.uid, parameterUid = 7L, newValue = "off")

        coVerify(exactly = 1) { sendNewTaskUseCase(deviceUuid, taskTypeOne.uid, mapOf(7L to "off")) }
    }
}
