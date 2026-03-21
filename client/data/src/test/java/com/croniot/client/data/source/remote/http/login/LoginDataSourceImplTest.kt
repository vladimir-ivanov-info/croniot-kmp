package com.croniot.client.data.source.remote.http.login

import Outcome
import com.croniot.client.core.models.auth.AuthError
import croniot.messages.LoginDto
import croniot.models.LoginResultDto
import croniot.models.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginDataSourceImplTest {

    private val api: LoginApi = mockk()
    private lateinit var dataSource: LoginDataSourceImpl

    private val loginRequest = LoginDto(
        email = "user@example.com",
        password = "secret",
        deviceUuid = "device-uuid",
        deviceToken = null,
        deviceProperties = emptyMap(),
    )

    private val validBody = LoginResultDto(
        result = Result(success = true),
        accountDto = null,
        token = "token",
    )

    @BeforeEach
    fun setUp() {
        dataSource = LoginDataSourceImpl(api)
    }

    @Test
    fun `successful response returns Ok with body`() = runTest {
        coEvery { api.login(any()) } returns Response.success(validBody)

        val result = dataSource.login(loginRequest)

        assertInstanceOf(Outcome.Ok::class.java, result)
        assertEquals(validBody, (result as Outcome.Ok).value)
    }

    @Test
    fun `null body on success returns Err Server`() = runTest {
        coEvery { api.login(any()) } returns Response.success(null)

        val result = dataSource.login(loginRequest)

        assertInstanceOf(Outcome.Err::class.java, result)
        val err = (result as Outcome.Err).error
        assertInstanceOf(AuthError.Server::class.java, err)
    }

    @Test
    fun `ConnectException maps to AuthError Network`() = runTest {
        coEvery { api.login(any()) } throws ConnectException("refused")

        val result = dataSource.login(loginRequest)

        assertEquals(Outcome.Err(AuthError.Network), result)
    }

    @Test
    fun `UnknownHostException maps to AuthError Network`() = runTest {
        coEvery { api.login(any()) } throws UnknownHostException("unknown host")

        val result = dataSource.login(loginRequest)

        assertEquals(Outcome.Err(AuthError.Network), result)
    }

    @Test
    fun `SocketTimeoutException maps to AuthError Network`() = runTest {
        coEvery { api.login(any()) } throws SocketTimeoutException("timeout")

        val result = dataSource.login(loginRequest)

        assertEquals(Outcome.Err(AuthError.NetworkTiemout), result)
    }

    @Test
    fun `HTTP 401 maps to AuthError InvalidCredentials`() = runTest {
        val errorResponse = Response.error<LoginResultDto>(401, "Unauthorized".toResponseBody())
        coEvery { api.login(any()) } returns errorResponse

        val result = dataSource.login(loginRequest)

        assertEquals(Outcome.Err(AuthError.InvalidCredentials), result)
    }

    @Test
    fun `HTTP 500 maps to AuthError Server`() = runTest {
        val errorResponse = Response.error<LoginResultDto>(500, "Internal Server Error".toResponseBody())
        coEvery { api.login(any()) } returns errorResponse

        val result = dataSource.login(loginRequest)

        assertInstanceOf(Outcome.Err::class.java, result)
        assertInstanceOf(AuthError.Server::class.java, (result as Outcome.Err).error)
    }

    @Test
    fun `unexpected exception maps to AuthError Unknown`() = runTest {
        coEvery { api.login(any()) } throws RuntimeException("unexpected")

        val result = dataSource.login(loginRequest)

        assertEquals(Outcome.Err(AuthError.Unknown), result)
    }
}
