package com.croniot.client.domain.usecases

import Outcome
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.testing.fakes.FakeTasksRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SendNewTaskUseCaseImplTest {

    @Test
    fun `returns Ok when repository accepts the task`() = runTest {
        val repository = FakeTasksRepository(sendNewTaskOutcome = Outcome.Ok(Unit))
        val useCase = SendNewTaskUseCaseImpl(repository)

        val result = useCase("device-1", 10L, mapOf(1L to "value"))

        assertThat(result).isEqualTo(Outcome.Ok(Unit))
    }

    @Test
    fun `propagates error from repository`() = runTest {
        val error = TaskError.Remote(RemoteError.Unreachable)
        val repository = FakeTasksRepository(sendNewTaskOutcome = Outcome.Err(error))
        val useCase = SendNewTaskUseCaseImpl(repository)

        val result = useCase("device-1", 10L, emptyMap())

        assertThat(result).isEqualTo(Outcome.Err(error))
    }

    @Test
    fun `builds task with given device task type and parameters`() = runTest {
        val repository = FakeTasksRepository()
        val useCase = SendNewTaskUseCaseImpl(repository)

        val result = useCase("device-7", 20L, mapOf(3L to "on"))

        assertThat(result).isEqualTo(Outcome.Ok(Unit))
    }
}
