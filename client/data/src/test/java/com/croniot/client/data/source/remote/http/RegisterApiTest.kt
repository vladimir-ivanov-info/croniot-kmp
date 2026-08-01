package com.croniot.client.data.source.remote.http

import croniot.messages.MessageRegisterAccount
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

class RegisterApiTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun api(handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): RegisterApi {
        val engine = MockEngine(handler)
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        return RegisterApi(client)
    }

    @Test
    fun `WHEN registerAccount is called THEN it posts to the register endpoint and returns success result`() = runTest {
        val message = MessageRegisterAccount(
            accountUuid = "acc-1",
            nickname = "nick",
            email = "user@example.com",
            password = "pass",
        )
        var capturedUrl = ""
        var capturedMethod: HttpMethod? = null
        val registerApi = api { request ->
            capturedUrl = request.url.encodedPath
            capturedMethod = request.method
            respond(
                content = ByteReadChannel(json.encodeToString(Result(success = true))),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = registerApi.registerAccount(message)

        assertEquals(true, result.success)
        assertEquals("/api/register_account", capturedUrl)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun `WHEN a business error occurs THEN registerAccount returns failure result with message`() = runTest {
        val message = MessageRegisterAccount("acc-1", "nick", "user@example.com", "pass")
        val registerApi = api {
            respond(
                content = ByteReadChannel(json.encodeToString(Result(success = false, message = "Email taken"))),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = registerApi.registerAccount(message)

        assertEquals(false, result.success)
        assertEquals("Email taken", result.message)
    }
}
