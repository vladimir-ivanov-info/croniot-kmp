package com.croniot.client.features.tasktypes.presentation.create_task.parameter

import com.croniot.client.domain.usecases.RequestTaskStateInfoSyncUseCase
import com.croniot.testing.fakes.FakeTasksRepository
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.time.ZonedDateTime

/**
 * Targets the pure, private `computeSyncState` function directly via reflection, bypassing
 * `initialize()` entirely (see StatefulParameterViewModelTest for why the polling loop it starts
 * is deliberately not exercised under a TestDispatcher). This function has no coroutines and no
 * loop, so it's safe and deterministic to call directly.
 */
class StatefulParameterViewModelSyncStateTest {

    private lateinit var viewModel: StatefulParameterViewModel
    private lateinit var computeSyncStateMethod: Method

    private data class SyncStateResult(val isSynced: Boolean, val shouldRequestSync: Boolean)

    @BeforeEach
    fun setUp() {
        viewModel = StatefulParameterViewModel(
            tasksRepository = FakeTasksRepository(),
            requestTaskStateInfoSyncUseCase = mockk<RequestTaskStateInfoSyncUseCase>(),
        )
        computeSyncStateMethod = StatefulParameterViewModel::class.java
            .getDeclaredMethod("computeSyncState", ZonedDateTime::class.java)
            .also { it.isAccessible = true }
    }

    private fun compute(dateTime: ZonedDateTime?): SyncStateResult {
        val result = computeSyncStateMethod.invoke(viewModel, dateTime)
        val clazz = result.javaClass
        val isSynced = clazz.getDeclaredField("isSynced").also { it.isAccessible = true }.getBoolean(result)
        val shouldRequestSync = clazz.getDeclaredField("shouldRequestSync").also { it.isAccessible = true }.getBoolean(result)
        return SyncStateResult(isSynced, shouldRequestSync)
    }

    @Test
    fun `WHEN dateTime is null THEN it is not synced and requests a sync`() {
        val result = compute(null)

        assertEquals(SyncStateResult(isSynced = false, shouldRequestSync = true), result)
    }

    @Test
    fun `WHEN dateTime is right now THEN it is synced and does not request a sync`() {
        val result = compute(ZonedDateTime.now())

        assertEquals(SyncStateResult(isSynced = true, shouldRequestSync = false), result)
    }

    @Test
    fun `WHEN dateTime is 4 seconds ago THEN it is synced but already requests a new sync`() {
        val result = compute(ZonedDateTime.now().minusSeconds(4))

        assertEquals(SyncStateResult(isSynced = true, shouldRequestSync = true), result)
    }

    @Test
    fun `WHEN dateTime is 6 seconds ago THEN it is not synced and requests a sync`() {
        val result = compute(ZonedDateTime.now().minusSeconds(6))

        assertEquals(SyncStateResult(isSynced = false, shouldRequestSync = true), result)
    }

    @Test
    fun `WHEN dateTime is 2 seconds ago THEN it is synced and does not yet request a sync`() {
        val result = compute(ZonedDateTime.now().minusSeconds(2))

        assertEquals(SyncStateResult(isSynced = true, shouldRequestSync = false), result)
    }

    @Test
    fun `WHEN dateTime is in the future THEN it is synced and does not request a sync`() {
        val result = compute(ZonedDateTime.now().plusSeconds(10))

        assertEquals(SyncStateResult(isSynced = true, shouldRequestSync = false), result)
    }
}
