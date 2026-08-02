package com.croniot.client.data.repositories

import Outcome
import com.croniot.client.domain.models.auth.AuthError
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import croniot.models.LoginResultDto
import croniot.models.Result
import croniot.models.dto.AccountDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthRepositoryImplTest {

    private val loginDataSource: LoginDataSource = mockk()
    private lateinit var repository: AuthRepositoryImpl

    private val validAccountDto = AccountDto(
        uuid = "acc-uuid",
        nickname = "tester",
        email = "test@test.com",
        devices = emptyList(),
    )
    private val validToken = "jwt-token-abc"
    private val validRefreshToken = "refresh-token-xyz"
    private val validExpiresAt = 1_800_000_000L

    @BeforeEach
    fun setUp() {
        repository = AuthRepositoryImpl(loginDataSource)
    }

    private fun loginParams() = Triple("test@test.com", "password", "device-uuid")

    @Test
    fun `WHEN account and tokens are present THEN login returns Ok with LoginResult`() = runTest {
        val dto = LoginResultDto(
            result = Result(success = true),
            accountDto = validAccountDto,
            token = validToken,
            refreshToken = validRefreshToken,
            accessTokenExpiresAtEpochSeconds = validExpiresAt,
        )
        coEvery { loginDataSource.login(any()) } returns Outcome.Ok(dto)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertInstanceOf(Outcome.Ok::class.java, result)
        val ok = result as Outcome.Ok
        assertEquals(validToken, ok.value.tokens.accessToken)
        assertEquals(validRefreshToken, ok.value.tokens.refreshToken)
        assertEquals(validExpiresAt, ok.value.tokens.expiresAtEpochSeconds)
        assertEquals("acc-uuid", ok.value.account.uuid)
    }

    @Test
    fun `WHEN data source returns Err THEN login propagates it`() = runTest {
        coEvery { loginDataSource.login(any()) } returns Outcome.Err(AuthError.Network)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertEquals(Outcome.Err(AuthError.Network), result)
    }

    @Test
    fun `WHEN result success is false THEN login returns InvalidCredentials`() = runTest {
        val dto = LoginResultDto(
            result = Result(success = false),
            accountDto = null,
            token = null,
        )
        coEvery { loginDataSource.login(any()) } returns Outcome.Ok(dto)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertEquals(Outcome.Err(AuthError.InvalidCredentials), result)
    }

    @Test
    fun `WHEN account is null but result is success THEN login returns AccountMissing`() = runTest {
        val dto = LoginResultDto(
            result = Result(success = true),
            accountDto = null,
            token = validToken,
            refreshToken = validRefreshToken,
            accessTokenExpiresAtEpochSeconds = validExpiresAt,
        )
        coEvery { loginDataSource.login(any()) } returns Outcome.Ok(dto)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertEquals(Outcome.Err(AuthError.AccountMissing), result)
    }

    @Test
    fun `WHEN token is null but account is present THEN login returns TokenMissing`() = runTest {
        val dto = LoginResultDto(
            result = Result(success = true),
            accountDto = validAccountDto,
            token = null,
        )
        coEvery { loginDataSource.login(any()) } returns Outcome.Ok(dto)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertEquals(Outcome.Err(AuthError.TokenMissing), result)
    }

    @Test
    fun `WHEN refreshToken is missing THEN login returns TokenMissing`() = runTest {
        val dto = LoginResultDto(
            result = Result(success = true),
            accountDto = validAccountDto,
            token = validToken,
            refreshToken = null,
            accessTokenExpiresAtEpochSeconds = validExpiresAt,
        )
        coEvery { loginDataSource.login(any()) } returns Outcome.Ok(dto)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertEquals(Outcome.Err(AuthError.TokenMissing), result)
    }

    @Test
    fun `WHEN expiresAt is missing THEN login returns TokenMissing`() = runTest {
        val dto = LoginResultDto(
            result = Result(success = true),
            accountDto = validAccountDto,
            token = validToken,
            refreshToken = validRefreshToken,
            accessTokenExpiresAtEpochSeconds = null,
        )
        coEvery { loginDataSource.login(any()) } returns Outcome.Ok(dto)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertEquals(Outcome.Err(AuthError.TokenMissing), result)
    }

    @Test
    fun `WHEN deviceToken and deviceProperties are provided THEN login passes them through to the data source`() = runTest {
        val dto = LoginResultDto(
            result = Result(success = true),
            accountDto = validAccountDto,
            token = validToken,
            refreshToken = validRefreshToken,
            accessTokenExpiresAtEpochSeconds = validExpiresAt,
        )
        coEvery { loginDataSource.login(any()) } returns Outcome.Ok(dto)
        val properties = mapOf("model" to "Pixel")

        repository.login("test@test.com", "password", "device-uuid", "device-token-123", properties)

        io.mockk.coVerify(exactly = 1) {
            loginDataSource.login(
                match { it.deviceToken == "device-token-123" && it.deviceProperties == properties },
            )
        }
    }
}
