package com.croniot.client.data.source.remote.http

import com.croniot.client.core.config.Constants
import croniot.messages.MessageAddTask
import croniot.messages.MessageRequestTaskStateInfoSync
import croniot.models.Result
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class TaskApi(private val http: HttpClient) {

    suspend fun requestTaskConfigurations(deviceUuid: String): List<TaskDto> =
        http.get(taskConfigurationPath(deviceUuid)).body()

    suspend fun requestTaskStateInfoHistory(
        deviceUuid: String,
        limit: Int,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): List<TaskStateInfoHistoryEntryDto> =
        http.get(taskStateInfoHistoryPath(deviceUuid)) {
            parameter("limit", limit)
            parameter("before", before)
            parameter("beforeId", beforeId)
            parameter("taskTypeUid", taskTypeUid)
        }.body()

    suspend fun requestTaskStateInfoHistoryCount(
        deviceUuid: String,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Int =
        http.get(taskStateInfoHistoryCountPath(deviceUuid)) {
            parameter("before", before)
            parameter("beforeId", beforeId)
            parameter("taskTypeUid", taskTypeUid)
        }.body()

    suspend fun addTask(message: MessageAddTask): Result =
        http.post(Constants.ENDPOINT_ADD_TASK) {
            contentType(ContentType.Application.Json)
            setBody(message)
        }.body()

    suspend fun requestTaskStateInfoSync(message: MessageRequestTaskStateInfoSync): Result =
        http.post(Constants.ENDPOINT_REQUEST_TASK_STATE_INFO_SYNC) {
            contentType(ContentType.Application.Json)
            setBody(message)
        }.body()

    private fun taskConfigurationPath(deviceUuid: String): String =
        Constants.ENDPOINT_TASK_CONFIGURATION.replace("{deviceUuid}", deviceUuid)

    private fun taskStateInfoHistoryPath(deviceUuid: String): String =
        Constants.ENDPOINT_TASK_STATE_INFO_HISTORY.replace("{deviceUuid}", deviceUuid)

    private fun taskStateInfoHistoryCountPath(deviceUuid: String): String =
        Constants.ENDPOINT_TASK_STATE_INFO_HISTORY_COUNT.replace("{deviceUuid}", deviceUuid)
}
