package com.croniot.client.domain.usecases.ble

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.models.session.AppSession
import com.croniot.client.domain.usecases.StopDeviceListenersUseCase
import com.croniot.testing.fakes.FakeAppSessionRepository
import com.croniot.testing.fakes.FakeSensorDataRepository
import com.croniot.testing.fakes.FakeTasksRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ActivateBleOnlyModeUseCaseTest {

    @Test
    fun `WHEN ActivateBleOnlyModeUseCase is invoked THEN it stops device listeners and activates ble-only mode`() = runTest {
        val sensorDataRepository = FakeSensorDataRepository()
        val tasksRepository = FakeTasksRepository()
        val appSessionRepository = FakeAppSessionRepository()
        val useCase = ActivateBleOnlyModeUseCase(
            stopDeviceListenersUseCase = StopDeviceListenersUseCase(sensorDataRepository, tasksRepository),
            appSessionRepository = appSessionRepository,
        )

        useCase()

        assertThat(sensorDataRepository.stopAllListenersCalls).isEqualTo(1)
        assertThat(tasksRepository.stopAllListenersCalls).isEqualTo(1)
        assertThat(appSessionRepository.session.value).isEqualTo(AppSession.BleOnly)
    }
}
