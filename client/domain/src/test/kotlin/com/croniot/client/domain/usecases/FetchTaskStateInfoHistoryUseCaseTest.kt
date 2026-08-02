package com.croniot.client.domain.usecases

import Outcome
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.testing.fakes.FakeTasksRepository
import croniot.models.TaskKey
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class FetchTaskStateInfoHistoryUseCaseTest {

    private fun entry() = TaskStateInfoHistoryEntry(
        stateInfoId = 1L,
        taskKey = TaskKey("device-1", 10L, 1L),
        stateInfo = TaskStateInfo(dateTime = ZonedDateTime.now(), state = "RUNNING", progress = 0.5, errorMessage = ""),
    )

    @Test
    fun `WHEN repository fetch succeeds THEN it returns the history entries`() = runTest {
        val entries = listOf(entry())
        val repository = FakeTasksRepository(historyOutcome = Outcome.Ok(entries))
        val useCase = FetchTaskStateInfoHistoryUseCase(repository)

        val result = useCase("device-1", limit = 10)

        assertThat(result).isEqualTo(Outcome.Ok(entries))
    }

    @Test
    fun `WHEN repository returns an error THEN it propagates that error`() = runTest {
        val error = TaskError.Remote(RemoteError.Unreachable)
        val repository = FakeTasksRepository(historyOutcome = Outcome.Err(error))
        val useCase = FetchTaskStateInfoHistoryUseCase(repository)

        val result = useCase("device-1", limit = 10)

        assertThat(result).isEqualTo(Outcome.Err(error))
    }

    @Test
    fun `WHEN repository has no history THEN it returns an empty list`() = runTest {
        val repository = FakeTasksRepository(historyOutcome = Outcome.Ok(emptyList()))
        val useCase = FetchTaskStateInfoHistoryUseCase(repository)

        val result = useCase("device-1", limit = 10, before = "2024-01-01", beforeId = 5L, taskTypeUid = 99L)

        assertThat(result).isEqualTo(Outcome.Ok(emptyList()))
    }
}
