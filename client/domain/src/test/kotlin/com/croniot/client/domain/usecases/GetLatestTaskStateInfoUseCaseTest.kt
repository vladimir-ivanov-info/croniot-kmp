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
    fun `WHEN repository has no state info THEN it returns null`() {
        val repository = FakeTasksRepository()
        val useCase = GetLatestTaskStateInfoUseCase(repository)

        val result = useCase("device-1", 10L)

        assertThat(result).isNull()
    }

    @Test
    fun `WHEN repository has state info THEN it returns that state info`() {
        val info = TaskStateInfo(dateTime = ZonedDateTime.now(), state = "RUNNING", progress = 0.5, errorMessage = "")
        val repository = FakeTasksRepository(latestTaskStateInfo = info)
        val useCase = GetLatestTaskStateInfoUseCase(repository)

        val result = useCase("device-1", 10L)

        assertThat(result).isEqualTo(info)
    }
}
