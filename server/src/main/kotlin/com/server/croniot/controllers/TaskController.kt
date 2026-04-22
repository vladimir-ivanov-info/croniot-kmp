package com.server.croniot.controllers

import com.server.croniot.application.ApplicationScope
import com.server.croniot.application.DomainException
import com.server.croniot.data.mappers.toDto
import com.server.croniot.data.repositories.TaskRepository
import com.server.croniot.mqtt.MqttController
import com.server.croniot.services.DeviceService
import com.server.croniot.services.TaskService
import com.server.croniot.services.TaskTypeService
import croniot.measure
import croniot.messages.MessageAddTask
import croniot.messages.MessageFactory
import croniot.messages.MessageRequestTaskStateInfoSync
import croniot.models.TaskProgressUpdate
import croniot.models.TaskStateInfo
import croniot.models.errors.DomainError
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

class TaskController @Inject constructor(
    private val taskService: TaskService,
    private val taskTypeService: TaskTypeService,
    private val deviceService: DeviceService,
    private val tasksRepository: TaskRepository,
    private val applicationScope: ApplicationScope,
) {

    private val logger = KotlinLogging.logger {}

    fun addTaskProgress(deviceUuid: String, taskProgressUpdate: TaskProgressUpdate) {
        try {
            measure("messageArrived processing", log = { logger.debug { it } }) {
                val t0 = System.currentTimeMillis()

                // println("TIME: $t0.")

                val taskUid = taskProgressUpdate.taskUid ?: return
                val taskTypeUid = taskProgressUpdate.taskTypeUid
                val taskProgress = taskProgressUpdate.progress
                val taskState = taskProgressUpdate.state
                val errorMessage = taskProgressUpdate.errorMessage

                val device = deviceService.getByUuid(deviceUuid) ?: return
                // if (!taskTypeService.exists(device.uuid, taskTypeUid)) return

                val deviceId = deviceService.getId(deviceUuid) ?: return
                val taskTypeId = taskTypeService.getId(deviceId, taskTypeUid) ?: return
                val sentAt = taskService.iotSendTimestamps.remove("$deviceUuid:$taskTypeUid")
                // if (sentAt != null) {
                // println("[RTT] IoT round-trip (server→IoT→server): ${t0 - sentAt}ms (state=$taskState)")
                // }
                // println("[RTT] addTaskProgress DB lookups: ${System.currentTimeMillis() - t0}ms (state=$taskState, taskUid=$taskUid)")

                val existingTask = tasksRepository.get(deviceUuid, taskTypeUid, taskUid)

                if (existingTask != null) {
                    measure("existingTask processing", log = { logger.debug { it } }) {
                        handleExistingTask(
                            deviceUuid,
                            taskTypeUid,
                            taskUid,
                            taskTypeId,
                            taskState,
                            taskProgress,
                            errorMessage
                        )
                    }
                } else {
                    measure("newTask processing", log = { logger.debug { it } }) {
                        handleNewTask(
                            deviceUuid,
                            taskTypeId,
                            taskTypeUid,
                            taskState,
                            taskProgress,
                            errorMessage
                        )
                    }
                }
                // println("[RTT] addTaskProgress TOTAL: ${System.currentTimeMillis() - t0}ms (state=$taskState)")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error processing task progress for device $deviceUuid" }
        }
    }

    private fun handleNewTask(
        deviceUuid: String,
        taskTypeId: Long,
        taskTypeUid: Long,
        taskState: String,
        taskProgress: Double,
        errorMessage: String,
    ) {
        val t0 = System.currentTimeMillis()
        val task = taskService.create(taskTypeId, taskTypeUid) ?: return
        // println("[RTT] handleNewTask create: ${System.currentTimeMillis() - t0}ms")

        val stateInfo = TaskStateInfo(
            taskTypeUid,
            ZonedDateTime.now(),
            taskState,
            taskProgress,
            errorMessage,
        )
        taskService.createTaskState(stateInfo, taskTypeId)
        // println("[RTT] handleNewTask createState: ${System.currentTimeMillis() - t0}ms")

        val taskWithState = task.copy(mostRecentStateInfo = stateInfo)
        measure("newTask MQTT sending", log = { logger.debug { it } }) {
            applicationScope.launch {
                MqttController.sendNewTask(deviceUuid, taskWithState)
                // println("[RTT] handleNewTask MQTT sent: ${System.currentTimeMillis() - t0}ms (state=$taskState)")
            }
        }
    }

    private fun handleExistingTask(
        deviceUuid: String,
        taskTypeUid: Long,
        taskUid: Long,
        taskTypeId: Long,
        taskState: String,
        taskProgress: Double,
        errorMessage: String,
    ) {
        val t0 = System.currentTimeMillis()
        val task = tasksRepository.get(deviceUuid, taskTypeUid, taskUid) ?: return
        logger.debug { "handleExistingTask get: ${System.currentTimeMillis() - t0}ms" }

        val stateInfo = TaskStateInfo(
            task.uid,
            ZonedDateTime.now(),
            taskState,
            taskProgress,
            errorMessage,
        )
        taskService.createTaskState(stateInfo, taskTypeId)
        logger.debug { "handleExistingTask createState: ${System.currentTimeMillis() - t0}ms" }

        val stateInfoDto = stateInfo.toDto()
        applicationScope.launch {
            MqttController.sendNewTaskStateInfo(deviceUuid, taskTypeUid, taskUid, stateInfoDto)
            logger.debug { "handleExistingTask MQTT sent: ${System.currentTimeMillis() - t0}ms (state=$taskState)" }
        }
    }

    suspend fun addTask(call: ApplicationCall) {
        val message = call.receiveText()
        val messageAddTask = MessageFactory.fromJson<MessageAddTask>(message)
        val result = taskService.addTask(messageAddTask)
        call.respond(result)
    }

    suspend fun getTaskConfigurations(call: ApplicationCall) {
        val deviceUuid = call.parameters["deviceUuid"]
            ?: throw DomainException(DomainError.Validation("deviceUuid", "Missing deviceUuid"))

        val taskConfigurations = taskService.getTasksByDeviceUuid(deviceUuid)

        if (taskConfigurations.isNotEmpty()) {
            call.respond(taskConfigurations)
        } else {
            throw DomainException(DomainError.NotFound("task configurations for $deviceUuid"))
        }
    }

    suspend fun getTaskStateInfoHistory(call: ApplicationCall) {
        val deviceUuid = call.parameters["deviceUuid"]
            ?: throw DomainException(DomainError.Validation("deviceUuid", "Missing deviceUuid"))

        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        val before = parseBefore(call.request.queryParameters["before"])
        val beforeId = call.request.queryParameters["beforeId"]?.toLongOrNull()
<<<<<<< HEAD
        val taskTypeUids = call.request.queryParameters["taskTypeUids"]
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.takeIf { it.isNotEmpty() }
        val dateFrom = parseBefore(call.request.queryParameters["dateFrom"])
        val dateTo = parseBefore(call.request.queryParameters["dateTo"])

        val history = taskService.getTaskStateInfoHistory(
            deviceUuid,
            limit,
            before,
            beforeId,
            taskTypeUids,
            dateFrom,
            dateTo
        )
=======
        val taskTypeUid = call.request.queryParameters["taskTypeUid"]?.toLongOrNull()

        val history = taskService.getTaskStateInfoHistory(deviceUuid, limit, before, beforeId, taskTypeUid)
>>>>>>> 67a5a19 (Major migration april)
        call.respond(history)
    }

    suspend fun getTaskStateInfoHistoryCount(call: ApplicationCall) {
        val deviceUuid = call.parameters["deviceUuid"]
            ?: throw DomainException(DomainError.Validation("deviceUuid", "Missing deviceUuid"))

        val before = parseBefore(call.request.queryParameters["before"])
        val beforeId = call.request.queryParameters["beforeId"]?.toLongOrNull()
<<<<<<< HEAD
        val taskTypeUids = call.request.queryParameters["taskTypeUids"]
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.takeIf { it.isNotEmpty() }
        val dateFrom = parseBefore(call.request.queryParameters["dateFrom"])
        val dateTo = parseBefore(call.request.queryParameters["dateTo"])
        val total = taskService.getTaskStateInfoHistoryCount(
            deviceUuid,
            before,
            beforeId,
            taskTypeUids,
            dateFrom,
            dateTo
        )
=======
        val taskTypeUid = call.request.queryParameters["taskTypeUid"]?.toLongOrNull()
        val total = taskService.getTaskStateInfoHistoryCount(deviceUuid, before, beforeId, taskTypeUid)
>>>>>>> 67a5a19 (Major migration april)
        call.respond(total)
    }

    suspend fun requestTaskStateInfoSync(call: ApplicationCall) {
        val message = call.receiveText()
        val messageAddTask = MessageFactory.fromJson<MessageRequestTaskStateInfoSync>(message)

        val result = taskService.requestTaskStateInfoSync(
            messageAddTask.deviceUuid,
            messageAddTask.taskTypeUid.toLong(),
        )
        call.respond(result)
    }

    private fun parseBefore(raw: String?): java.time.OffsetDateTime? {
        if (raw == null) return null
        return raw.toLongOrNull()?.let {
            java.time.Instant.ofEpochMilli(it).atOffset(java.time.ZoneOffset.UTC)
        } ?: runCatching {
            java.time.OffsetDateTime.parse(raw)
        }.getOrNull()
    }
}
