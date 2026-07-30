package com.croniot.client.domain.usecases

import Outcome
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.testing.fakes.FakeTasksRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RequestTaskStateInfoSyncUseCaseImplTest {

    @Test
    fun `returns Ok when repository confirms sync request`() = runTest {
        val repository = FakeTasksRepository(requestSyncOutcome = Outcome.Ok(Unit))
        val useCase = RequestTaskStateInfoSyncUseCaseImpl(repository)

        val result = useCase("device-1", 10L)

        assertThat(result).isEqualTo(Outcome.Ok(Unit))
    }

    @Test
    fun `propagates error from repository`() = runTest {
        val error = TaskError.Remote(RemoteError.Unreachable)
        val repository = FakeTasksRepository(requestSyncOutcome = Outcome.Err(error))
        val useCase = RequestTaskStateInfoSyncUseCaseImpl(repository)

        val result = useCase("device-1", 10L)

        assertThat(result).isEqualTo(Outcome.Err(error))
    }
}
