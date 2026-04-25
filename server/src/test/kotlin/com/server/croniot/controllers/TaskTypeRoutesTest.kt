package com.server.croniot.controllers

import com.server.croniot.application.installStatusPages
import com.server.croniot.services.TaskTypeService
import croniot.messages.MessageFactory
import croniot.messages.MessageRegisterTaskType
import croniot.models.Result as DomainResult
import croniot.models.TaskType
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

class TaskTypeRoutesTest {

    private val message = MessageRegisterTaskType(
        deviceUuid = "device-uuid",
        deviceToken = "token",
        taskType = TaskType(
            uid = 42L,
            name = "Rotate log",
            description = "Scheduled maintenance",
            parameters = emptyList(),
        ),
    )

    private fun Application.testModule(service: TaskTypeService) {
        install(ContentNegotiation) { json(MessageFactory.json) }
        installStatusPages()
        val controller = TaskTypeController(service)
        routing { post("/api/register_task_type") { controller.registerTaskType(call) } }
    }

    @Test
    fun `POST register_task_type returns 200 and success payload when service accepts`() = testApplication {
        val service = mockk<TaskTypeService>()
        every { service.registerTaskType(any()) } returns DomainResult(success = true, message = "Task 42 registered")
        application { testModule(service) }

        val response = client.post("/api/register_task_type") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(message))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertTrue(payload.success)
        assertEquals("Task 42 registered", payload.message)
    }

    @Test
    fun `POST register_task_type returns 200 with failure payload when token is incorrect`() = testApplication {
        val service = mockk<TaskTypeService>()
        every { service.registerTaskType(any()) } returns DomainResult(
            success = false,
            message = "Incorrect device or token for task type register process.",
        )
        application { testModule(service) }

        val response = client.post("/api/register_task_type") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(message))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertFalse(payload.success)
    }
}
