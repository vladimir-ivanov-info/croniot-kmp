package com.croniot.client.domain.usecases

import com.croniot.client.domain.repositories.RegisterAccountRepository
import croniot.models.Result

class RegisterAccountUseCase(
    private val repository: RegisterAccountRepository,
) {
    suspend operator fun invoke(nickname: String, email: String, password: String): Result =
        repository.registerAccount(nickname, email, password)
}