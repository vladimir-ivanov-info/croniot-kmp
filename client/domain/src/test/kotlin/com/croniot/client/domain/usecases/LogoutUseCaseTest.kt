package com.croniot.client.domain.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.testing.fakes.FakeAppSessionRepository
import com.croniot.testing.fakes.FakeSensorDataRepository
import com.croniot.testing.fakes.FakeSessionRepository
import com.croniot.testing.fakes.FakeTasksRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class LogoutUseCaseTest {

    @Test
    fun `WHEN LogoutUseCase is invoked THEN it stops device listeners, clears session and clears app session`() = runTest {
        val sensorDataRepository = FakeSensorDataRepository()
        val tasksRepository = FakeTasksRepository()
        val sessionRepository = FakeSessionRepository()
        val appSessionRepository = FakeAppSessionRepository()
        val useCase = LogoutUseCase(
            sessionRepository = sessionRepository,
            stopDeviceListenersUseCase = StopDeviceListenersUseCase(sensorDataRepository, tasksRepository),
            appSessionRepository = appSessionRepository,
        )

        useCase()

        assertThat(sensorDataRepository.stopAllListenersCalls).isEqualTo(1)
        assertThat(tasksRepository.stopAllListenersCalls).isEqualTo(1)
        assertThat(sessionRepository.clearAllExceptDeviceUuidCalls).isEqualTo(1)
        assertThat(appSessionRepository.clearCalls).isEqualTo(1)
    }

    @Test
    fun `WHEN there were no active listeners THEN it still clears session and app session`() = runTest {
        val sessionRepository = FakeSessionRepository()
        val appSessionRepository = FakeAppSessionRepository()
        val useCase = LogoutUseCase(
            sessionRepository = sessionRepository,
            stopDeviceListenersUseCase = StopDeviceListenersUseCase(FakeSensorDataRepository(), FakeTasksRepository()),
            appSessionRepository = appSessionRepository,
        )

        useCase()

        assertThat(sessionRepository.clearAllExceptDeviceUuidCalls).isEqualTo(1)
        assertThat(appSessionRepository.clearCalls).isEqualTo(1)
    }
}
