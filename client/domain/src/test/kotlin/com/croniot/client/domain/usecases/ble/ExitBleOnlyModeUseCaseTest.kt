package com.croniot.client.domain.usecases.ble

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.models.session.AppSession
import com.croniot.testing.fakes.FakeAppSessionRepository
import com.croniot.testing.fakes.FakeBleDevicesRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ExitBleOnlyModeUseCaseTest {

    @Test
    fun `disconnects all ble devices and clears the app session`() = runTest {
        val bleDevicesRepository = FakeBleDevicesRepository()
        val appSessionRepository = FakeAppSessionRepository(initial = AppSession.BleOnly)
        val useCase = ExitBleOnlyModeUseCase(
            bleDevicesRepository = bleDevicesRepository,
            appSessionRepository = appSessionRepository,
        )

        useCase()

        assertThat(bleDevicesRepository.disconnectAllCalls).isEqualTo(1)
        assertThat(appSessionRepository.session.value).isEqualTo(AppSession.None)
    }
}
