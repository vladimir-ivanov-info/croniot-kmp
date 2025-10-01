package com.croniot.client.features.login.domain.usecase

import com.croniot.client.core.models.auth.AuthError
import com.croniot.client.core.models.auth.AuthSession
import com.croniot.client.core.models.auth.Outcome
import com.croniot.client.core.util.DevicePropertiesController
import com.croniot.client.data.repositories.AccountRepository
import com.croniot.client.data.repositories.AuthRepository
import com.croniot.client.data.repositories.LocalDataRepository
import com.croniot.client.data.repositories.SessionRepository

class LogInUseCase(
    private val authRepository: AuthRepository,
    private val localDataRepository: LocalDataRepository,
    private val sessionRepository: SessionRepository,
    private val accountRepository: AccountRepository
) {

    suspend operator fun invoke(email: String, password: String): Outcome<Unit, AuthError> {
        var result : Outcome<Unit, AuthError>

        if (email.isBlank() || password.isBlank()) {
            result = Outcome.Err(AuthError.InvalidCredentials)
        } else {
            val deviceUuid = localDataRepository.getLocalDeviceUuid()
            val deviceToken = localDataRepository.getLocalDeviceToken()
            val deviceProperties = DevicePropertiesController.getDeviceDetails()

            //TODO los !!
            val loginResult = authRepository.login(email, password, deviceUuid!!, deviceToken, deviceProperties)

            result = when (loginResult) {
                is Outcome.Ok -> {
                    val token = loginResult.value.token

                    sessionRepository.save(
                        session = AuthSession(email = email, token = token)
                    )
                    accountRepository.save(account = loginResult.value.account)
                    localDataRepository.savePassword(password = password)
                    Outcome.Ok(Unit)
                }
                is Outcome.Err -> loginResult
            }
        }
        return result
    }
}
