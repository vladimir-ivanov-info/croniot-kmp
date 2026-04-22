package com.server.croniot.controllers

import com.server.croniot.application.installStatusPages
import com.server.croniot.services.SensorTypeService
import croniot.messages.MessageFactory
import croniot.messages.MessageRegisterSensorType
import croniot.models.Result as DomainResult
import croniot.models.SensorType
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

class SensorTypeRoutesTest {

    private val message = MessageRegisterSensorType(
        deviceUuid = "device-uuid",
        deviceToken = "token",
        sensorType = SensorType(
            uid = 7L,
            name = "Temperature",
            description = "degrees C",
            parameters = emptyList(),
        ),
    )

    private fun Application.testModule(service: SensorTypeService) {
        install(ContentNegotiation) { json(MessageFactory.json) }
        installStatusPages()
        val controller = SensorTypeController(service)
        routing { post("/api/register_sensor_type") { controller.registerSensorType(call) } }
    }

    @Test
    fun `POST register_sensor_type returns 200 and success payload when service accepts`() = testApplication {
        val service = mockk<SensorTypeService>()
        every { service.registerSensorType(any()) } returns DomainResult(success = true, message = "Sensor 7 registered")
        application { testModule(service) }

        val response = client.post("/api/register_sensor_type") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(message))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertTrue(payload.success)
        assertEquals("Sensor 7 registered", payload.message)
    }

    @Test
    fun `POST register_sensor_type returns 200 with failure payload when token is incorrect`() = testApplication {
        val service = mockk<SensorTypeService>()
        every { service.registerSensorType(any()) } returns DomainResult(
            success = false,
            message = "Incorrect device or token for sensor register process.",
        )
        application { testModule(service) }

        val response = client.post("/api/register_sensor_type") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(message))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertFalse(payload.success)
    }
}
