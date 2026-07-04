package com.croniot.client.domain.usecases

import Outcome
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.croniot.client.domain.LoginResult
import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.auth.AuthError
import com.croniot.client.domain.models.auth.AuthTokens
import com.croniot.testing.fakes.FakeAccountRepository
import com.croniot.testing.fakes.FakeAuthRepository
import com.croniot.testing.fakes.FakeDevicePropertiesProvider
import com.croniot.testing.fakes.FakeLocalDataRepository
import com.croniot.testing.fakes.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class LogInUseCaseTest {

    private val baseAccount = Account(
        uuid = "account-1",
        nickname = "nick",
        email = "user@example.com",
        devices = emptyList(),
    )
    private val tokens = AuthTokens(
        accessToken = "access-1",
        refreshToken = "refresh-1",
        expiresAtEpochSeconds = 1_900_000_000L,
    )

    @Test
    fun `returns InvalidCredentials when email is blank`() = runTest {
        val useCase = buildUseCase()

        val result = useCase(email = "", password = "password")

        assertThat(result).isEqualTo(Outcome.Err(AuthError.InvalidCredentials))
    }

    @Test
    fun `returns InvalidCredentials when password is blank`() = runTest {
        val useCase = buildUseCase()

        val result = useCase(email = "user@example.com", password = "")

        assertThat(result).isEqualTo(Outcome.Err(AuthError.InvalidCredentials))
    }

    @Test
    fun `happy path persists session, tokens and account`() = runTest {
        val authRepository = FakeAuthRepository(
            loginOutcome = Outcome.Ok(LoginResult(account = baseAccount, tokens = tokens)),
        )
        val sessionRepository = FakeSessionRepository()
        val accountRepository = FakeAccountRepository()
        val useCase = buildUseCase(
            authRepository = authRepository,
            sessionRepository = sessionRepository,
            accountRepository = accountRepository,
        )

        val result = useCase(email = "user@example.com", password = "secret")

        assertThat(result).isInstanceOf(Outcome.Ok::class)
        assertThat(authRepository.loginInvocations).hasSize(1)
        assertThat(authRepository.loginInvocations.first())
            .prop(FakeAuthRepository.LoginInvocation::email).isEqualTo("user@example.com")
        assertThat(sessionRepository.savedSession?.email).isEqualTo("user@example.com")
        assertThat(sessionRepository.savedSession?.token).isEqualTo(tokens.accessToken)
        assertThat(sessionRepository.savedTokens).isEqualTo(tokens)
        assertThat(accountRepository.saveCalls).isEqualTo(1)
    }

    @Test
    fun `propagates error outcome from auth repository`() = runTest {
        val error = AuthError.InvalidCredentials
        val useCase = buildUseCase(
            authRepository = FakeAuthRepository(loginOutcome = Outcome.Err(error)),
        )

        val result = useCase(email = "user@example.com", password = "wrong")

        assertThat(result).isEqualTo(Outcome.Err(error))
    }

    private fun buildUseCase(
        authRepository: FakeAuthRepository = FakeAuthRepository(),
        sessionRepository: FakeSessionRepository = FakeSessionRepository(),
        accountRepository: FakeAccountRepository = FakeAccountRepository(),
        localDataRepository: FakeLocalDataRepository = FakeLocalDataRepository(
            deviceUuid = "device-uuid",
            deviceToken = "device-token",
        ),
        devicePropertiesProvider: FakeDevicePropertiesProvider = FakeDevicePropertiesProvider(),
    ) = LogInUseCase(
        authRepository = authRepository,
        localDataRepository = localDataRepository,
        sessionRepository = sessionRepository,
        accountRepository = accountRepository,
        devicePropertiesProvider = devicePropertiesProvider,
    )
}
