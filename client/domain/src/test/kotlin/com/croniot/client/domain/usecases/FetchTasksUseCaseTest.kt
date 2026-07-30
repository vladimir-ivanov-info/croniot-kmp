package com.croniot.client.domain.usecases

import Outcome
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.Task
import com.croniot.testing.fakes.FakeTasksRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FetchTasksUseCaseTest {

    @Test
    fun `returns tasks from repository on success`() = runTest {
        val tasks = listOf(Task(deviceUuid = "device-1", taskTypeUid = 10L, uid = 1L))
        val repository = FakeTasksRepository(fetchTasksOutcome = Outcome.Ok(tasks))
        val useCase = FetchTasksUseCase(repository)

        val result = useCase("device-1")

        assertThat(result).isEqualTo(Outcome.Ok(tasks))
    }

    @Test
    fun `propagates error from repository`() = runTest {
        val error = TaskError.Remote(RemoteError.Unreachable)
        val repository = FakeTasksRepository(fetchTasksOutcome = Outcome.Err(error))
        val useCase = FetchTasksUseCase(repository)

        val result = useCase("device-1")

        assertThat(result).isEqualTo(Outcome.Err(error))
    }

    @Test
    fun `returns empty list when repository has no tasks`() = runTest {
        val repository = FakeTasksRepository(fetchTasksOutcome = Outcome.Ok(emptyList()))
        val useCase = FetchTasksUseCase(repository)

        val result = useCase("device-1")

        assertThat(result).isEqualTo(Outcome.Ok(emptyList()))
    }

    @Test
    fun `passes through the requested device uuid`() = runTest {
        val repository = FakeTasksRepository()
        val useCase = FetchTasksUseCase(repository)

        useCase("device-42")

        assertThat(repository.fetchTasksInvocations).isEqualTo(mutableListOf("device-42"))
    }
}
