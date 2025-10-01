package com.croniot.client.domain.usecases

import com.croniot.client.data.repositories.SessionRepository

class LogoutUseCase(
    private val sessionRepository: SessionRepository
) {

    suspend operator fun invoke() {
        sessionRepository.clearAllExceptDeviceUuid()
    }

}
