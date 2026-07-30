package com.server.croniot.controllers

import com.server.croniot.application.AUTH_JWT_REALM
import com.server.croniot.application.DomainException
import com.server.croniot.application.JwtConfig
import com.server.croniot.application.installStatusPages
import com.server.croniot.mqtt.MqttController
import com.server.croniot.services.FeatureFlagService
import com.server.croniot.testsupport.Fixtures
import croniot.messages.MessageFactory
import croniot.models.dto.FeatureFlagDto
import croniot.models.errors.DomainError
import croniot.models.errors.ErrorResponse
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeatureFlagRoutesTest {

    private val jwtConfig = JwtConfig(Fixtures.secrets())

    @BeforeEach
    fun setUp() {
        mockkObject(MqttController)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(MqttController)
    }

    private fun Application.testModule(service: FeatureFlagService) {
        install(ContentNegotiation) { json(MessageFactory.json) }
        install(Authentication) {
            jwt(AUTH_JWT_REALM) {
                verifier(jwtConfig.verifier())
                validate { credential ->
                    val subject = credential.payload.subject
                    val hasAudience = credential.payload.audience.contains(jwtConfig.audience)
                    if (!subject.isNullOrBlank() && hasAudience) JWTPrincipal(credential.payload) else null
                }
            }
        }
        installStatusPages()

        val controller = FeatureFlagController(service)
        routing {
            get("/api/feature_flags") { controller.getAllFlags(call) }
            authenticate(AUTH_JWT_REALM) {
                put("/api/admin/feature_flags/{name}") { controller.setFlag(call) }
            }
        }
    }

    private fun adminToken() =
        jwtConfig.issueAccessToken(accountId = 1L, email = "admin@example.com", isAdmin = true).token

    private fun nonAdminToken() =
        jwtConfig.issueAccessToken(accountId = 2L, email = "user@example.com", isAdmin = false).token

    @Test
    fun `GET feature_flags returns 200 with all flags from service`() = testApplication {
        val service = mockk<FeatureFlagService>()
        every { service.getAll() } returns listOf(FeatureFlagDto("dark_mode", true, "desc"))
        application { testModule(service) }

        val response = client.get("/api/feature_flags")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = MessageFactory.fromJson<List<FeatureFlagDto>>(response.bodyAsText())
        assertEquals(1, body.size)
        assertEquals("dark_mode", body.first().name)
        assertTrue(body.first().enabled)
    }

    @Test
    fun `PUT admin feature_flags updates flag, broadcasts over MQTT and returns 200 when caller is admin`() =
        testApplication {
            val service = mockk<FeatureFlagService>()
            val updated = FeatureFlagDto("dark_mode", true, "desc")
            every { service.setEnabled("dark_mode", true) } returns updated
            coEvery { MqttController.broadcastFeatureFlagUpdate(updated) } returns Unit
            application { testModule(service) }

            val response = client.put("/api/admin/feature_flags/dark_mode") {
                header(HttpHeaders.Authorization, "Bearer ${adminToken()}")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(MessageFactory.toJson(SetFlagRequest(enabled = true)))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = MessageFactory.fromJson<FeatureFlagDto>(response.bodyAsText())
            assertEquals("dark_mode", body.name)
            assertTrue(body.enabled)
            coVerify(exactly = 1) { MqttController.broadcastFeatureFlagUpdate(updated) }
        }

    @Test
    fun `PUT admin feature_flags returns 401 UNAUTHORIZED and does not broadcast when caller is not admin`() =
        testApplication {
            val service = mockk<FeatureFlagService>()
            application { testModule(service) }

            val response = client.put("/api/admin/feature_flags/dark_mode") {
                header(HttpHeaders.Authorization, "Bearer ${nonAdminToken()}")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(MessageFactory.toJson(SetFlagRequest(enabled = true)))
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            val error = MessageFactory.fromJson<ErrorResponse>(response.bodyAsText())
            assertEquals(DomainError.Unauthorized.CODE, error.code)
            coVerify(exactly = 0) { MqttController.broadcastFeatureFlagUpdate(any()) }
        }

    @Test
    fun `PUT admin feature_flags returns 401 UNAUTHORIZED via auth challenge when no token is sent`() =
        testApplication {
            val service = mockk<FeatureFlagService>()
            application { testModule(service) }

            val response = client.put("/api/admin/feature_flags/dark_mode") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(MessageFactory.toJson(SetFlagRequest(enabled = true)))
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            coVerify(exactly = 0) { MqttController.broadcastFeatureFlagUpdate(any()) }
        }

    @Test
    fun `PUT admin feature_flags returns 404 NOT_FOUND when flag name does not exist`() = testApplication {
        val service = mockk<FeatureFlagService>()
        every {
            service.setEnabled("unknown", true)
        } throws DomainException(DomainError.NotFound("feature_flag 'unknown'"))
        application { testModule(service) }

        val response = client.put("/api/admin/feature_flags/unknown") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken()}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(SetFlagRequest(enabled = true)))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        val error = MessageFactory.fromJson<ErrorResponse>(response.bodyAsText())
        assertEquals(DomainError.NotFound.CODE, error.code)
        coVerify(exactly = 0) { MqttController.broadcastFeatureFlagUpdate(any()) }
    }
}
