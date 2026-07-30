package com.croniot.client.data.source.remote.http

import croniot.messages.MessageAddTask
import croniot.messages.MessageRequestTaskStateInfoSync
import croniot.models.Result
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoHistoryEntryDto
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TaskApiTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun api(handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): TaskApi {
        val engine = MockEngine(handler)
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        return TaskApi(client)
    }

    @Test
    fun `requestTaskConfigurations hits path with device uuid substituted`() = runTest {
        var capturedUrl = ""
        val taskApi = api { request ->
            capturedUrl = request.url.encodedPath
            respond(
                content = ByteReadChannel(json.encodeToString(listOf(TaskDto(uid = 1L, taskTypeUid = 10L)))),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = taskApi.requestTaskConfigurations("device-1")

        assertEquals("/taskConfiguration/device-1", capturedUrl)
        assertEquals(1, result.size)
        assertEquals(10L, result.first().taskTypeUid)
    }

    @Test
    fun `requestTaskConfigurations returns empty list when device has no tasks`() = runTest {
        val taskApi = api {
            respond(
                content = ByteReadChannel(json.encodeToString(emptyList<TaskDto>())),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = taskApi.requestTaskConfigurations("device-1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `requestTaskStateInfoHistory sends limit before beforeId and taskTypeUid as query params`() = runTest {
        var capturedQuery = ""
        val entry = TaskStateInfoHistoryEntryDto(
            stateInfoId = 1L,
            taskUid = 1L,
            taskTypeUid = 10L,
            dateTime = ZonedDateTime.now(),
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        )
        val taskApi = api { request ->
            capturedQuery = request.url.encodedQuery
            respond(
                content = ByteReadChannel(json.encodeToString(listOf(entry))),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = taskApi.requestTaskStateInfoHistory("device-1", limit = 10, before = "2024-01-01", beforeId = 5L, taskTypeUid = 20L)

        assertTrue(capturedQuery.contains("limit=10"))
        assertTrue(capturedQuery.contains("beforeId=5"))
        assertTrue(capturedQuery.contains("taskTypeUid=20"))
        assertEquals(1, result.size)
    }

    @Test
    fun `requestTaskStateInfoHistoryCount returns integer count from server`() = runTest {
        val taskApi = api {
            respond(
                content = ByteReadChannel("42"),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = taskApi.requestTaskStateInfoHistoryCount("device-1", before = null, beforeId = null, taskTypeUid = null)

        assertEquals(42, result)
    }

    @Test
    fun `addTask posts to add_task endpoint and returns result`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedUrl = ""
        val taskApi = api { request ->
            capturedMethod = request.method
            capturedUrl = request.url.encodedPath
            respond(
                content = ByteReadChannel(json.encodeToString(Result(success = true))),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = taskApi.addTask(MessageAddTask(deviceUuid = "device-1", taskTypeUid = "10", parametersValues = emptyMap()))

        assertEquals(true, result.success)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertEquals("/api/add_task", capturedUrl)
    }

    @Test
    fun `requestTaskStateInfoSync posts to sync endpoint and returns result`() = runTest {
        var capturedUrl = ""
        val taskApi = api { request ->
            capturedUrl = request.url.encodedPath
            respond(
                content = ByteReadChannel(json.encodeToString(Result(success = true))),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = taskApi.requestTaskStateInfoSync(MessageRequestTaskStateInfoSync(deviceUuid = "device-1", taskTypeUid = "10"))

        assertEquals(true, result.success)
        assertEquals("/api/request_task_state_info_sync", capturedUrl)
    }

    @Test
    fun `addTask returns failure result when server rejects the task`() = runTest {
        val taskApi = api {
            respond(
                content = ByteReadChannel(json.encodeToString(Result(success = false, message = "Invalid task type"))),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = taskApi.addTask(MessageAddTask("device-1", "999", emptyMap()))

        assertEquals(false, result.success)
        assertEquals("Invalid task type", result.message)
    }
}
