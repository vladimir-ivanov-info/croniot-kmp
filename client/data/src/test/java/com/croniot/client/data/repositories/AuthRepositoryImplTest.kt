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

    @BeforeEach
    fun setUp() {
        repository = AuthRepositoryImpl(loginDataSource)
    }

    private fun loginParams() = Triple("test@test.com", "password", "device-uuid")

    @Test
    fun `login returns Ok with LoginResult when account and token are present`() = runTest {
        val dto = LoginResultDto(
            result = Result(success = true),
            accountDto = validAccountDto,
            token = validToken,
        )
        coEvery { loginDataSource.login(any()) } returns Outcome.Ok(dto)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertInstanceOf(Outcome.Ok::class.java, result)
        val ok = result as Outcome.Ok
        assertEquals(validToken, ok.value.token)
        assertEquals("acc-uuid", ok.value.account.uuid)
    }

    @Test
    fun `login propagates Err from data source`() = runTest {
        coEvery { loginDataSource.login(any()) } returns Outcome.Err(AuthError.Network)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertEquals(Outcome.Err(AuthError.Network), result)
    }

    @Test
    fun `login returns InvalidCredentials when result success is false`() = runTest {
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
    fun `login returns AccountMissing when account is null but result is success`() = runTest {
        val dto = LoginResultDto(
            result = Result(success = true),
            accountDto = null,
            token = validToken,
        )
        coEvery { loginDataSource.login(any()) } returns Outcome.Ok(dto)

        val (email, password, deviceUuid) = loginParams()
        val result = repository.login(email, password, deviceUuid, null, emptyMap())

        assertEquals(Outcome.Err(AuthError.AccountMissing), result)
    }

    @Test
    fun `login returns TokenMissing when token is null but account is present`() = runTest {
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
}
