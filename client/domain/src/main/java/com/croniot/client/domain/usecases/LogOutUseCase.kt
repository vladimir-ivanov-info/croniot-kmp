package com.croniot.client.domain.usecases

import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.SessionRepository

class LogoutUseCase(
    private val sessionRepository: SessionRepository,
    private val stopDeviceListenersUseCase: StopDeviceListenersUseCase,
    private val appSessionRepository: AppSessionRepository,
) {

    suspend operator fun invoke() {
        stopDeviceListenersUseCase()
        sessionRepository.clearAllExceptDeviceUuid()
        appSessionRepository.clear()
    }
}
