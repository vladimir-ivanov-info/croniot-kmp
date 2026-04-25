package com.server.croniot.controllers

import com.server.croniot.application.ApplicationScope
import com.server.croniot.application.installStatusPages
import com.server.croniot.data.repositories.TaskRepository
import com.server.croniot.services.DeviceService
import com.server.croniot.services.TaskService
import com.server.croniot.services.TaskTypeService
import croniot.messages.MessageAddTask
import croniot.messages.MessageFactory
import croniot.messages.MessageRequestTaskStateInfoSync
import croniot.models.Result as DomainResult
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import croniot.models.errors.DomainError
import croniot.models.errors.ErrorResponse
import io.ktor.client.request.get
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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TaskRoutesTest {

    private val applicationScope = ApplicationScope()

    @AfterEach
    fun tearDown() {
        applicationScope.shutdown()
    }

    private fun Application.testModule(
        taskService: TaskService,
        taskTypeService: TaskTypeService = mockk(),
        deviceService: DeviceService = mockk(),
        taskRepository: TaskRepository = mockk(),
    ) {
        install(ContentNegotiation) { json(MessageFactory.json) }
        installStatusPages()

        val controller = TaskController(
            taskService = taskService,
            taskTypeService = taskTypeService,
            deviceService = deviceService,
            tasksRepository = taskRepository,
            applicationScope = applicationScope,
        )

        routing {
            post("/api/add_task") { controller.addTask(call) }
            get("/taskConfiguration/{deviceUuid}") { controller.getTaskConfigurations(call) }
            get("/taskStateInfoHistory/{deviceUuid}") { controller.getTaskStateInfoHistory(call) }
            get("/taskStateInfoHistoryCount/{deviceUuid}") { controller.getTaskStateInfoHistoryCount(call) }
            post("/api/request_task_state_info_sync") { controller.requestTaskStateInfoSync(call) }
        }
    }

    @Test
    fun `POST add_task delegates to service and returns its Result`() = testApplication {
        val taskService = mockk<TaskService>()
        val captured = slot<MessageAddTask>()
        every { taskService.addTask(capture(captured)) } returns DomainResult(success = true, message = "")
        application { testModule(taskService) }

        val body = MessageAddTask(
            deviceUuid = "device-uuid",
            taskTypeUid = "42",
            parametersValues = mapOf(1L to "v"),
        )
        val response = client.post("/api/add_task") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(body))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertTrue(payload.success)
        assertEquals("device-uuid", captured.captured.deviceUuid)
        assertEquals("42", captured.captured.taskTypeUid)
    }

    @Test
    fun `GET taskConfiguration returns 200 with list when service has configurations`() = testApplication {
        val taskService = mockk<TaskService>()
        val taskDto = TaskDto(uid = 1L, taskTypeUid = 42L)
        every { taskService.getTasksByDeviceUuid("device-uuid") } returns listOf(taskDto)
        application { testModule(taskService) }

        val response = client.get("/taskConfiguration/device-uuid")

        assertEquals(HttpStatusCode.OK, response.status)
        val list = MessageFactory.fromJson<List<TaskDto>>(response.bodyAsText())
        assertEquals(1, list.size)
        assertEquals(42L, list.first().taskTypeUid)
    }

    @Test
    fun `GET taskConfiguration returns 404 NOT_FOUND when service returns an empty list`() = testApplication {
        val taskService = mockk<TaskService>()
        every { taskService.getTasksByDeviceUuid(any()) } returns emptyList()
        application { testModule(taskService) }

        val response = client.get("/taskConfiguration/unknown-device")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val error = MessageFactory.fromJson<ErrorResponse>(response.bodyAsText())
        assertEquals(DomainError.NotFound.CODE, error.code)
        assertTrue(error.message.contains("unknown-device"))
    }

    @Test
    fun `GET taskStateInfoHistory passes query params and returns serialized history`() = testApplication {
        val taskService = mockk<TaskService>()
        val entry = TaskStateInfoHistoryEntryDto(
            stateInfoId = 10L,
            taskUid = 100L,
            taskTypeUid = 42L,
            dateTime = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneOffset.UTC),
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        )
        val expectedBefore = OffsetDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(1745136000000),
            ZoneOffset.UTC,
        )
        every {
            taskService.getTaskStateInfoHistory(
                deviceUuid = "device-uuid",
                limit = 25,
                before = expectedBefore,
                beforeId = 55L,
                taskTypeUid = 42L,
            )
        } returns listOf(entry)
        application { testModule(taskService) }

        val response = client.get(
            "/taskStateInfoHistory/device-uuid" +
                "?limit=25&before=1745136000000&beforeId=55&taskTypeUid=42",
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val history = MessageFactory.fromJson<List<TaskStateInfoHistoryEntryDto>>(response.bodyAsText())
        assertEquals(1, history.size)
        assertEquals(10L, history.first().stateInfoId)
        verify(exactly = 1) {
            taskService.getTaskStateInfoHistory(
                deviceUuid = "device-uuid",
                limit = 25,
                before = expectedBefore,
                beforeId = 55L,
                taskTypeUid = 42L,
            )
        }
    }

    @Test
    fun `GET taskStateInfoHistory defaults limit to 50 and passes null query params when absent`() = testApplication {
        val taskService = mockk<TaskService>()
        every {
            taskService.getTaskStateInfoHistory(
                deviceUuid = "device-uuid",
                limit = 50,
                before = null,
                beforeId = null,
                taskTypeUid = null,
            )
        } returns emptyList()
        application { testModule(taskService) }

        val response = client.get("/taskStateInfoHistory/device-uuid")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) {
            taskService.getTaskStateInfoHistory(
                deviceUuid = "device-uuid",
                limit = 50,
                before = null,
                beforeId = null,
                taskTypeUid = null,
            )
        }
    }

    @Test
    fun `GET taskStateInfoHistoryCount returns serialized count`() = testApplication {
        val taskService = mockk<TaskService>()
        every {
            taskService.getTaskStateInfoHistoryCount("device-uuid", any(), any(), any())
        } returns 123
        application { testModule(taskService) }

        val response = client.get("/taskStateInfoHistoryCount/device-uuid")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("123", response.bodyAsText().trim())
    }

    @Test
    fun `POST request_task_state_info_sync forwards to service and returns its Result`() = testApplication {
        val taskService = mockk<TaskService>()
        every { taskService.requestTaskStateInfoSync("device-uuid", 42L) } returns DomainResult(success = true)
        application { testModule(taskService) }

        val response = client.post("/api/request_task_state_info_sync") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                MessageFactory.toJson(
                    MessageRequestTaskStateInfoSync(deviceUuid = "device-uuid", taskTypeUid = "42"),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertTrue(payload.success)
        verify(exactly = 1) { taskService.requestTaskStateInfoSync("device-uuid", 42L) }
    }
}
