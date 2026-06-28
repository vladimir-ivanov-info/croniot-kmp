package com.croniot.client.domain.usecases.ble

import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.usecases.StopDeviceListenersUseCase

class ActivateBleOnlyModeUseCase(
    private val stopDeviceListenersUseCase: StopDeviceListenersUseCase,
    private val appSessionRepository: AppSessionRepository,
) {

    suspend operator fun invoke() {
        stopDeviceListenersUseCase()
        appSessionRepository.activateBleOnlyMode()
    }
}
