package com.croniot.client.domain.usecases

import Outcome
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.auth.AuthError
import com.croniot.client.domain.models.auth.AuthSession
import com.croniot.client.domain.DevicePropertiesProvider
import com.croniot.client.domain.repositories.AccountRepository
import com.croniot.client.domain.repositories.AuthRepository
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.SessionRepository

class LogInUseCase(
    private val authRepository: AuthRepository,
    private val localDataRepository: LocalDataRepository,
    private val sessionRepository: SessionRepository,
    private val accountRepository: AccountRepository,
    private val devicePropertiesProvider: DevicePropertiesProvider,
) {

    suspend operator fun invoke(email: String, password: String): Outcome<Unit, AuthError> {
        var result: Outcome<Unit, AuthError>

        if (email.isBlank() || password.isBlank()) {
            result = Outcome.Err(AuthError.InvalidCredentials)
        } else {
            val deviceUuid = localDataRepository.getLocalDeviceUuid()
            val deviceToken = localDataRepository.getLocalDeviceToken()
            val deviceProperties = devicePropertiesProvider.getDeviceDetails()

            // TODO los !!
            val loginResult = authRepository.login(email, password, deviceUuid!!, deviceToken, deviceProperties)

            result = when (loginResult) {
                is Outcome.Ok -> {
                    val accessToken = loginResult.value.tokens.accessToken

                    sessionRepository.save(
                        session = AuthSession(email = email, token = accessToken),
                    )
                    sessionRepository.saveTokens(loginResult.value.tokens)
                   // accountRepository.save(account = loginResult.value.account)
                    accountRepository.save(
                        account = loginResult.value.account.copy(
                            devices = loginResult.value.account.devices + listOf(
                                Device(
                                    uuid = "mock-1",
                                    name = "Coffee Maker",
                                    description = ""
                                ),
                                Device(
                                    uuid = "mock-2",
                                    name = "E-Scooter",
                                    description = ""
                                ),
                                Device(
                                    uuid = "mock-3",
                                    name = "Solar Panel",
                                    description = ""
                                ),
                                Device(
                                    uuid = "mock-4",
                                    name = "Fish Tank",
                                    description = ""
                                ),
                                Device(
                                    uuid = "mock-5",
                                    name = "Pet Feeder",
                                    description = ""
                                )
                            )
                        )
                    )
                    Outcome.Ok(Unit)
                }
                is Outcome.Err -> loginResult
            }
        }
        return result
    }
}
