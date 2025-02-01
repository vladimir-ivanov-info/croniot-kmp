package com.server.croniot.controllers

import com.server.croniot.mqtt.MqttController
import com.server.croniot.services.DeviceService
import com.server.croniot.services.TaskService
import com.server.croniot.services.TaskTypeService
import croniot.messages.MessageAddTask
import croniot.messages.MessageFactory
import croniot.models.TaskProgressUpdate
import croniot.models.TaskStateInfo
import croniot.models.toDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

class TaskController @Inject constructor(
    private val taskService: TaskService,
    private val taskTypeService: TaskTypeService,
    private val deviceService: DeviceService,
) {

    //TODO refactor, make smaller
    fun addTaskProgress(deviceUuid: String, taskProgressUpdate: TaskProgressUpdate){
        try {
            val taskUid = taskProgressUpdate.taskUid
            val taskTypeUid = taskProgressUpdate.taskTypeUid
            val taskProgress = taskProgressUpdate.progress
            val taskState = taskProgressUpdate.state
            val errorMessage = taskProgressUpdate.errorMessage

            val device = deviceService.getByUuid(deviceUuid)

            if (device != null && taskUid != null) {
                val taskTypeExists = taskTypeService.exists(device, taskTypeUid) // 8 ms

                if (taskTypeExists) {
                    if (taskUid.toInt() == -1) {
                        val taskType = taskTypeService.get(device, taskTypeUid)
                        taskType?.let {
                            val task = taskService.create(device, taskType)

                            task?.let {
                                val taskStateEnum = taskState
                                val stateInfo = TaskStateInfo(
                                    ZonedDateTime.now(),
                                    taskStateEnum,
                                    taskProgress,
                                    errorMessage,
                                    task
                                )
                                taskService.createTaskState(stateInfo) //5-7 ms

                                val stateInfoDto = stateInfo.toDto()  //1816 ms
                                println("Time task state: ${stateInfoDto.state} ${ZonedDateTime.now()}")
                                CoroutineScope(Dispatchers.IO).launch {
                                    MqttController.sendNewTask(deviceUuid, task, stateInfo)
                                }
                            }
                        }
                    } else {
                        val task = taskService.getLazy(deviceUuid, taskTypeUid, taskUid) //1700 ms -> 1500 ms -> 3 ms
                        task?.let {
                            val stateInfo = TaskStateInfo(
                                ZonedDateTime.now(),
                                taskState,
                                taskProgress,
                                errorMessage,
                                task
                            )

                            taskService.createTaskState(stateInfo) //5-7 ms

                            val stateInfoDto = stateInfo.toDto()  //1816 ms
                            CoroutineScope(Dispatchers.IO).launch {
                                MqttController.sendNewTaskStateInfo(deviceUuid, stateInfoDto) //100-120 ms -> 90-100 -> 1 ms
                            }
                        }
                    }
                }
            }
        } catch (e : Exception){
            e.printStackTrace()
        }
    }

    suspend fun addTask(call: ApplicationCall){
        val message = call.receiveText();
        val messageAddTask = MessageFactory.fromJson<MessageAddTask>(message)
        val result = taskService.addTask(messageAddTask)
        call.respond(result)
    }

    suspend fun getTaskConfigurations(call: ApplicationCall){
        val deviceUuid = call.parameters["deviceUuid"] ?: call.respondText(
            "Missing or malformed deviceUuid",
            status = HttpStatusCode.BadRequest
        )

        val taskConfigurations = taskService.getTasksByDeviceUuid(deviceUuid.toString()) //57 ms

        if (taskConfigurations.isNotEmpty()) {
            call.respond(taskConfigurations)
        } else {
            call.respond(HttpStatusCode.NotFound, "No configurations found for UUID: $deviceUuid")
        }
    }

}