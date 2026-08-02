package com.croniot.client.domain.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.testing.fakes.FakeSensorDataRepository
import com.croniot.testing.fakes.FakeTasksRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StopDeviceListenersUseCaseTest {

    @Test
    fun `WHEN StopDeviceListenersUseCase is invoked THEN it stops both sensor and task listeners`() = runTest {
        val sensorDataRepository = FakeSensorDataRepository()
        val tasksRepository = FakeTasksRepository()
        val useCase = StopDeviceListenersUseCase(sensorDataRepository, tasksRepository)

        useCase()

        assertThat(sensorDataRepository.stopAllListenersCalls).isEqualTo(1)
        assertThat(tasksRepository.stopAllListenersCalls).isEqualTo(1)
    }
}
