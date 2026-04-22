package com.server.croniot.controllers

import com.server.croniot.application.DomainException
import com.server.croniot.application.RATE_LIMIT_AUTH
import com.server.croniot.application.installStatusPages
import com.server.croniot.services.LoginService
import croniot.messages.LoginDto
import croniot.messages.MessageFactory
import croniot.models.LogoutRequestDto
import croniot.models.RefreshTokenRequestDto
import croniot.models.RefreshTokenResultDto
import croniot.models.Result as DomainResult
import croniot.models.LoginResultDto
import croniot.models.errors.DomainError
import croniot.models.errors.ErrorResponse
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class LoginRoutesTest {

    private val loginDto = LoginDto(
        email = "user@example.com",
        password = "secret",
        deviceUuid = "device-uuid",
        deviceToken = null,
        deviceProperties = emptyMap(),
    )

    private fun Application.testModule(loginService: LoginService) {
        install(ContentNegotiation) { json(MessageFactory.json) }
        install(RateLimit) {
            register(RATE_LIMIT_AUTH) {
                rateLimiter(limit = 5, refillPeriod = 1.minutes)
            }
        }
        installStatusPages()

        val loginController = LoginController(loginService)
        routing {
            rateLimit(RATE_LIMIT_AUTH) {
                post("/api/login") { loginController.login(call) }
                post("/api/token/refresh") { loginController.refreshToken(call) }
            }
            post("/api/logout") { loginController.logout(call) }
        }
    }

    @Test
    fun `POST api login returns 200 with LoginResultDto on happy path`() = testApplication {
        val loginService = mockk<LoginService>()
        every { loginService.login(any()) } returns LoginResultDto(
            result = DomainResult(success = true, message = "ok"),
            accountDto = null,
            token = "access-token",
            refreshToken = "refresh-token",
            accessTokenExpiresAtEpochSeconds = 1_234_567L,
        )
        application { testModule(loginService) }

        val response = client.post("/api/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(loginDto))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = MessageFactory.fromJson<LoginResultDto>(response.bodyAsText())
        assertEquals("access-token", body.token)
        assertEquals("refresh-token", body.refreshToken)
        assertTrue(body.result.success)
    }

    @Test
    fun `POST api login returns 401 with INVALID_CREDENTIALS when service throws InvalidCredentials`() = testApplication {
        val loginService = mockk<LoginService>()
        every { loginService.login(any()) } throws DomainException(DomainError.InvalidCredentials())
        application { testModule(loginService) }

        val response = client.post("/api/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(loginDto))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val error = MessageFactory.fromJson<ErrorResponse>(response.bodyAsText())
        assertEquals(DomainError.InvalidCredentials.CODE, error.code)
    }

    @Test
    fun `POST api login returns 409 with CONFLICT when service throws Conflict`() = testApplication {
        val loginService = mockk<LoginService>()
        every { loginService.login(any()) } throws DomainException(DomainError.Conflict("Device already registered"))
        application { testModule(loginService) }

        val response = client.post("/api/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(loginDto))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val error = MessageFactory.fromJson<ErrorResponse>(response.bodyAsText())
        assertEquals(DomainError.Conflict.CODE, error.code)
        assertEquals("Device already registered", error.message)
    }

    @Test
    fun `POST api login returns 500 with INTERNAL for unhandled exceptions`() = testApplication {
        val loginService = mockk<LoginService>()
        every { loginService.login(any()) } throws IllegalStateException("boom")
        application { testModule(loginService) }

        val response = client.post("/api/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(loginDto))
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val error = MessageFactory.fromJson<ErrorResponse>(response.bodyAsText())
        assertEquals(DomainError.Internal.CODE, error.code)
    }

    @Test
    fun `POST api token refresh returns 200 with new tokens on happy path`() = testApplication {
        val loginService = mockk<LoginService>()
        every { loginService.refresh("old-refresh") } returns RefreshTokenResultDto(
            result = DomainResult(success = true),
            token = "new-access",
            refreshToken = "new-refresh",
            accessTokenExpiresAtEpochSeconds = 9_999L,
        )
        application { testModule(loginService) }

        val response = client.post("/api/token/refresh") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(RefreshTokenRequestDto(refreshToken = "old-refresh")))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = MessageFactory.fromJson<RefreshTokenResultDto>(response.bodyAsText())
        assertEquals("new-access", body.token)
        assertEquals("new-refresh", body.refreshToken)
    }

    @Test
    fun `POST api token refresh returns 401 UNAUTHORIZED when rotation fails`() = testApplication {
        val loginService = mockk<LoginService>()
        every { loginService.refresh(any()) } throws DomainException(DomainError.Unauthorized("Invalid or expired refresh token"))
        application { testModule(loginService) }

        val response = client.post("/api/token/refresh") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(RefreshTokenRequestDto(refreshToken = "broken")))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val error = MessageFactory.fromJson<ErrorResponse>(response.bodyAsText())
        assertEquals(DomainError.Unauthorized.CODE, error.code)
    }

    @Test
    fun `POST api logout returns 200 with success result`() = testApplication {
        val loginService = mockk<LoginService>()
        every { loginService.logout("refresh-to-revoke") } returns DomainResult(success = true)
        application { testModule(loginService) }

        val response = client.post("/api/logout") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(LogoutRequestDto(refreshToken = "refresh-to-revoke")))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertTrue(body.success)
    }

    @Test
    fun `POST api login responds 400 VALIDATION with field detail when service throws Validation`() = testApplication {
        val loginService = mockk<LoginService>()
        every { loginService.login(any()) } throws DomainException(
            DomainError.Validation(field = "email", message = "Email is required"),
        )
        application { testModule(loginService) }

        val response = client.post("/api/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(loginDto))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = MessageFactory.fromJson<ErrorResponse>(response.bodyAsText())
        assertEquals(DomainError.Validation.CODE, error.code)
        assertEquals("email", error.details["field"])
        assertNotNull(error.message)
    }
}
