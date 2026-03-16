package com.server.croniot.controllers

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
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

class TaskController @Inject constructor(
    private val taskService: TaskService,
    private val taskTypeService: TaskTypeService,
    private val deviceService: DeviceService,
    private val tasksRepository: TaskRepository
) {

    fun addTaskProgress(deviceUuid: String, taskProgressUpdate: TaskProgressUpdate) {
        try {
            measure("###SERVER MQTT messageArrived processing") {
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
                    measure("###SERVER MQTT existingTask messageArrived processing") {
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
                    measure("###SERVER MQTT newTask messageArrived processing") {
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
            e.printStackTrace()
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
        measure("###SERVER MQTT newTask MQTT sneding messageArrived processing") {
            CoroutineScope(Dispatchers.IO).launch {
                MqttController.sendNewTask(deviceUuid, taskWithState)
                //println("[RTT] handleNewTask MQTT sent: ${System.currentTimeMillis() - t0}ms (state=$taskState)")
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
        println("[RTT] handleExistingTask get: ${System.currentTimeMillis() - t0}ms")

        val stateInfo = TaskStateInfo(
            task.uid,
            ZonedDateTime.now(),
            taskState,
            taskProgress,
            errorMessage,
        )
        taskService.createTaskState(stateInfo, taskTypeId)
        println("[RTT] handleExistingTask createState: ${System.currentTimeMillis() - t0}ms")

        val stateInfoDto = stateInfo.toDto()
        CoroutineScope(Dispatchers.IO).launch {
            MqttController.sendNewTaskStateInfo(deviceUuid, taskTypeUid, taskUid, stateInfoDto)
            println("[RTT] handleExistingTask MQTT sent: ${System.currentTimeMillis() - t0}ms (state=$taskState)")
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
        if (deviceUuid == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing deviceUuid")
            return
        }

        val taskConfigurations = taskService.getTasksByDeviceUuid(deviceUuid)

        if (taskConfigurations.isNotEmpty()) {
            call.respond(taskConfigurations)
        } else {
            call.respond(HttpStatusCode.NotFound, "No configurations found for UUID: $deviceUuid")
        }
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
}
