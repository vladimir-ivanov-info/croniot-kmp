package com.croniot.client.data.source.remote.http.login

import Outcome
import com.croniot.client.domain.models.auth.AuthError
import croniot.messages.LoginDto
import croniot.models.LoginResultDto
import croniot.models.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.net.ConnectException
import java.net.UnknownHostException

class LoginDataSourceImplTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

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

    private fun dataSource(handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData): LoginDataSourceImpl {
        val engine = MockEngine(handler)
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
        }
        return LoginDataSourceImpl(LoginApi(client))
    }

    @Test
    fun `successful response returns Ok with body`() = runTest {
        val source = dataSource {
            respond(
                content = ByteReadChannel(json.encodeToString(validBody)),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = source.login(loginRequest)

        assertInstanceOf(Outcome.Ok::class.java, result)
        assertEquals(validBody, (result as Outcome.Ok).value)
    }

    @Test
    fun `ConnectException maps to AuthError Network`() = runTest {
        val source = dataSource { throw ConnectException("refused") }

        val result = source.login(loginRequest)

        assertEquals(Outcome.Err(AuthError.Network), result)
    }

    @Test
    fun `UnknownHostException maps to AuthError Network`() = runTest {
        val source = dataSource { throw UnknownHostException("unknown host") }

        val result = source.login(loginRequest)

        assertEquals(Outcome.Err(AuthError.Network), result)
    }

    @Test
    fun `HTTP 401 maps to AuthError InvalidCredentials`() = runTest {
        val source = dataSource {
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.Unauthorized,
            )
        }

        val result = source.login(loginRequest)

        assertEquals(Outcome.Err(AuthError.InvalidCredentials), result)
    }

    @Test
    fun `HTTP 500 maps to AuthError Server`() = runTest {
        val source = dataSource {
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.InternalServerError,
            )
        }

        val result = source.login(loginRequest)

        assertInstanceOf(Outcome.Err::class.java, result)
        assertInstanceOf(AuthError.Server::class.java, (result as Outcome.Err).error)
    }

    @Test
    fun `unexpected exception maps to AuthError Unknown`() = runTest {
        val source = dataSource { throw RuntimeException("unexpected") }

        val result = source.login(loginRequest)

        assertEquals(Outcome.Err(AuthError.Unknown), result)
    }
}
