package com.croniot.client.domain.usecases

import com.croniot.client.domain.repositories.SessionRepository

class LogoutUseCase(
    private val sessionRepository: SessionRepository,
    private val stopDeviceListenersUseCase: StopDeviceListenersUseCase,
) {

    suspend operator fun invoke() {
        stopDeviceListenersUseCase()
        sessionRepository.clearAllExceptDeviceUuid()
    }
}
