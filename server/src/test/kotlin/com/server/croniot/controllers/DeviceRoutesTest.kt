package com.server.croniot.controllers

import com.server.croniot.application.installStatusPages
import com.server.croniot.services.DeviceService
import croniot.messages.MessageFactory
import croniot.messages.MessageRegisterDevice
import croniot.models.Result as DomainResult
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
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceRoutesTest {

    private val message = MessageRegisterDevice(
        accountEmail = "user@example.com",
        accountPassword = "secret",
        deviceUuid = "device-uuid",
        deviceName = "Living Room Thermostat",
        deviceDescription = "Smart thermostat v2",
    )

    private fun Application.testModule(service: DeviceService) {
        install(ContentNegotiation) { json(MessageFactory.json) }
        installStatusPages()
        val controller = DeviceController(service)
        routing { post("/api/register_client") { controller.registerDevice(call) } }
    }

    @Test
    fun `POST register_client returns 200 and success with token when service accepts`() = testApplication {
        val service = mockk<DeviceService>()
        every { service.registerDevice(any()) } returns DomainResult(
            success = true,
            message = "generated-device-token",
        )
        application { testModule(service) }

        val response = client.post("/api/register_client") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(message))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertTrue(payload.success)
        assertEquals("generated-device-token", payload.message)
    }

    @Test
    fun `POST register_client returns 200 with failure when account does not exist`() = testApplication {
        val service = mockk<DeviceService>()
        every { service.registerDevice(any()) } returns DomainResult(
            success = false,
            message = "Account for user@example.com doesn't exist.",
        )
        application { testModule(service) }

        val response = client.post("/api/register_client") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(message))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertFalse(payload.success)
    }
}
