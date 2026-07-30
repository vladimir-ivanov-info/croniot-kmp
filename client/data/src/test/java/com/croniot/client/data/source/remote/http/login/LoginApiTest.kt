package com.croniot.client.data.source.remote.http.login

import croniot.messages.LoginDto
import croniot.models.LoginResultDto
import croniot.models.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginApiTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun api(handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): LoginApi {
        val engine = MockEngine(handler)
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        return LoginApi(client)
    }

    @Test
    fun `login posts to the login endpoint with json content type`() = runTest {
        var capturedUrl = ""
        var capturedMethod: HttpMethod? = null
        val body = LoginResultDto(result = Result(success = true), accountDto = null, token = "jwt")
        val loginApi = api { request ->
            capturedUrl = request.url.encodedPath
            capturedMethod = request.method
            respond(
                content = ByteReadChannel(json.encodeToString(body)),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = loginApi.login(LoginDto("user@example.com", "pass", "device-1", null, emptyMap()))

        assertEquals("/api/login", capturedUrl)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertEquals("jwt", result.token)
    }

    @Test
    fun `login deserializes a failed result without a token`() = runTest {
        val body = LoginResultDto(result = Result(success = false, message = "bad credentials"), accountDto = null, token = null)
        val loginApi = api {
            respond(
                content = ByteReadChannel(json.encodeToString(body)),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = loginApi.login(LoginDto("user@example.com", "pass", "device-1", null, emptyMap()))

        assertEquals(false, result.result.success)
        assertEquals(null, result.token)
    }
}
