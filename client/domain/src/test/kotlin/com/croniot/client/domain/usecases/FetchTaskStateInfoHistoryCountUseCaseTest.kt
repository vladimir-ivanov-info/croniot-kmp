package com.croniot.client.domain.usecases

import Outcome
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.testing.fakes.FakeTasksRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FetchTaskStateInfoHistoryCountUseCaseTest {

    @Test
    fun `returns count from repository`() = runTest {
        val repository = FakeTasksRepository(historyCountOutcome = Outcome.Ok(42))
        val useCase = FetchTaskStateInfoHistoryCountUseCase(repository)

        val result = useCase("device-1")

        assertThat(result).isEqualTo(Outcome.Ok(42))
    }

    @Test
    fun `propagates error from repository`() = runTest {
        val error = TaskError.Remote(RemoteError.Unreachable)
        val repository = FakeTasksRepository(historyCountOutcome = Outcome.Err(error))
        val useCase = FetchTaskStateInfoHistoryCountUseCase(repository)

        val result = useCase("device-1")

        assertThat(result).isEqualTo(Outcome.Err(error))
    }

    @Test
    fun `returns zero when repository has no history`() = runTest {
        val repository = FakeTasksRepository(historyCountOutcome = Outcome.Ok(0))
        val useCase = FetchTaskStateInfoHistoryCountUseCase(repository)

        val result = useCase("device-1", before = "2024-01-01", beforeId = 5L, taskTypeUid = 99L)

        assertThat(result).isEqualTo(Outcome.Ok(0))
    }
}
