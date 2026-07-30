package com.croniot.client.domain.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.testing.fakes.FakeTasksRepository
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GetLatestTaskStateInfoUseCaseTest {

    @Test
    fun `returns null when repository has no state info`() {
        val repository = FakeTasksRepository()
        val useCase = GetLatestTaskStateInfoUseCase(repository)

        val result = useCase("device-1", 10L)

        assertThat(result).isNull()
    }

    @Test
    fun `returns state info from repository when present`() {
        val info = TaskStateInfo(dateTime = ZonedDateTime.now(), state = "RUNNING", progress = 0.5, errorMessage = "")
        val repository = FakeTasksRepository(latestTaskStateInfo = info)
        val useCase = GetLatestTaskStateInfoUseCase(repository)

        val result = useCase("device-1", 10L)

        assertThat(result).isEqualTo(info)
    }
}
