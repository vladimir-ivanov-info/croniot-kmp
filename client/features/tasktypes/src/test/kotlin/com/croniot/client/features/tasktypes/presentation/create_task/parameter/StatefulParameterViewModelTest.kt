package com.croniot.client.features.tasktypes.presentation.create_task.parameter

import Outcome
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.usecases.RequestTaskStateInfoSyncUseCase
import com.croniot.testing.fakes.FakeTasksRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

/**
 * [StatefulParameterViewModel.initialize] launches an internal flow that loops forever
 * (`while (isActive) { emit(...); delay(1000) }`). To keep these tests deterministic without
 * hanging we deliberately do NOT put a [kotlinx.coroutines.test.TestDispatcher] behind
 * `Dispatchers.Main`:
 *
 * - `runTest` auto-detects when `Dispatchers.Main` is backed by a `TestDispatcher` and, in that
 *   case, reuses its `TestCoroutineScheduler` for the whole test (this is documented behavior of
 *   `runTest`). Because the ViewModel's infinite `delay(1000)` loop would then share that same
 *   scheduler, `runTest`'s own cleanup (`advanceUntilIdleOr`) would try to fully drain it and the
 *   test would spin forever (verified: it happens and blows up with `OutOfMemoryError`).
 * - Instead we set `Dispatchers.Main` to the plain [Dispatchers.Unconfined] dispatcher. This keeps
 *   the same "run eagerly on the calling thread up to the first real suspension point" semantics
 *   we need (so `viewModelScope.launch { ... }` runs the first flow emission synchronously by the
 *   time `initialize()` returns), but it is not a `TestDispatcher`, so `runTest` does not link its
 *   virtual scheduler to it. The pending `delay(1000)` continuation is then a real (not virtual)
 *   delay that nobody awaits, so it never affects the test's completion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatefulParameterViewModelTest {

    private val tasksRepository = FakeTasksRepository()
    private val requestTaskStateInfoSyncUseCase: RequestTaskStateInfoSyncUseCase = mockk()

    private lateinit var viewModel: StatefulParameterViewModel

    private val deviceUuid = "device-1"
    private val taskTypeUid = 1L

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        viewModel = StatefulParameterViewModel(
            tasksRepository = tasksRepository,
            requestTaskStateInfoSyncUseCase = requestTaskStateInfoSyncUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `recent task state info marks the parameter as synced without requesting a sync`() = runTest {
        tasksRepository.latestTaskStateInfoEmittedByIoT = TaskStateInfo(
            dateTime = ZonedDateTime.now().minusSeconds(1),
            state = "RUNNING",
            progress = 0.0,
            errorMessage = "",
        )

        viewModel.initialize(deviceUuid, taskTypeUid)

        assertEquals(true, viewModel.statefulTaskInfoParameterSynced.value)
        coVerify(exactly = 0) { requestTaskStateInfoSyncUseCase(any(), any()) }
    }

    @Test
    fun `stale task state info marks the parameter as not synced and requests a sync`() = runTest {
        tasksRepository.latestTaskStateInfoEmittedByIoT = TaskStateInfo(
            dateTime = ZonedDateTime.now().minusSeconds(10),
            state = "RUNNING",
            progress = 0.0,
            errorMessage = "",
        )
        coEvery { requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeUid) } returns Outcome.Ok(Unit)

        viewModel.initialize(deviceUuid, taskTypeUid)

        assertEquals(false, viewModel.statefulTaskInfoParameterSynced.value)
        coVerify(exactly = 1) { requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeUid) }
    }
}
