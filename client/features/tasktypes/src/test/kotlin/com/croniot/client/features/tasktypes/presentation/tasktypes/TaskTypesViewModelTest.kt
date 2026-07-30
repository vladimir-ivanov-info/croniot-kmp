package com.croniot.client.features.tasktypes.presentation.tasktypes

import Outcome
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.domain.usecases.GetLatestTaskStateInfoUseCase
import com.croniot.client.domain.usecases.ObserveTaskStateInfoUseCase
import com.croniot.client.domain.usecases.RequestTaskStateInfoSyncUseCase
import com.croniot.client.presentation.util.formatStateInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskTypesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fetchTasksUseCase: FetchTasksUseCase = mockk()
    private val requestTaskStateInfoSyncUseCase: RequestTaskStateInfoSyncUseCase = mockk()
    private val observeTaskStateInfoUseCase: ObserveTaskStateInfoUseCase = mockk()
    private val getLatestTaskStateInfoUseCase: GetLatestTaskStateInfoUseCase = mockk()

    private lateinit var viewModel: TaskTypesViewModel

    private val deviceUuid = "device-1"
    private val taskTypeOne = TaskType(uid = 1L, name = "Task 1", description = "desc-1", parameters = emptyList())
    private val taskTypeTwo = TaskType(uid = 2L, name = "Task 2", description = "desc-2", parameters = emptyList())

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskTypesViewModel(
            fetchTasksUseCase = fetchTasksUseCase,
            requestTaskStateInfoSyncUseCase = requestTaskStateInfoSyncUseCase,
            observeTaskStateInfoUseCase = observeTaskStateInfoUseCase,
            getLatestTaskStateInfoUseCase = getLatestTaskStateInfoUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize fetches tasks then requests sync for each task type`() = runTest(testDispatcher) {
        coEvery { fetchTasksUseCase(deviceUuid) } returns Outcome.Ok(emptyList<Task>())
        coEvery { requestTaskStateInfoSyncUseCase(any(), any()) } returns Outcome.Ok(Unit)

        viewModel.initialize(deviceUuid, listOf(taskTypeOne, taskTypeTwo))

        coVerify(exactly = 1) { fetchTasksUseCase(deviceUuid) }
        coVerify(exactly = 1) { requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeOne.uid) }
        coVerify(exactly = 1) { requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeTwo.uid) }
    }

    @Test
    fun `observeTaskTypeUpdates caches the StateFlow for the same key`() = runTest(testDispatcher) {
        every { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns emptyFlow()

        val first = viewModel.observeTaskTypeUpdates(deviceUuid, taskTypeOne)
        val second = viewModel.observeTaskTypeUpdates(deviceUuid, taskTypeOne)

        assertEquals(first, second)
        verify(exactly = 1) { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) }
    }

    @Test
    fun `getSecondaryText uses latest task state info for the initial value when it exists`() = runTest(testDispatcher) {
        val latestInfo = TaskStateInfo(
            dateTime = ZonedDateTime.now(),
            state = "RUNNING",
            progress = 42.0,
            errorMessage = "",
        )
        every { getLatestTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns latestInfo
        every { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns emptyFlow()

        val result = viewModel.getSecondaryText(deviceUuid, taskTypeOne)

        assertEquals(formatStateInfo(latestInfo), result.value)
    }

    @Test
    fun `getSecondaryText returns empty string when there is no latest task state info`() = runTest(testDispatcher) {
        every { getLatestTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns null
        every { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns emptyFlow()

        val result = viewModel.getSecondaryText(deviceUuid, taskTypeOne)

        assertEquals("", result.value)
    }

    @Test
    fun `observeTaskTypeUpdates with different task types returns different StateFlow instances`() = runTest(testDispatcher) {
        every { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns emptyFlow()
        every { observeTaskStateInfoUseCase(deviceUuid, taskTypeTwo.uid) } returns emptyFlow()

        val first = viewModel.observeTaskTypeUpdates(deviceUuid, taskTypeOne)
        val second = viewModel.observeTaskTypeUpdates(deviceUuid, taskTypeTwo)

        assertNotSame(first, second)
    }

    @Test
    fun `getSecondaryText caches the StateFlow for the same key`() = runTest(testDispatcher) {
        every { getLatestTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns null
        every { observeTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) } returns emptyFlow()

        val first = viewModel.getSecondaryText(deviceUuid, taskTypeOne)
        val second = viewModel.getSecondaryText(deviceUuid, taskTypeOne)

        assertSame(first, second)
        verify(exactly = 1) { getLatestTaskStateInfoUseCase(deviceUuid, taskTypeOne.uid) }
    }
}
