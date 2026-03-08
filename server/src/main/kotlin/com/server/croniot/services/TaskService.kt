package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.TaskRepository
import com.server.croniot.data.repositories.TaskTypeRepository
import com.server.croniot.mqtt.MqttController
import croniot.messages.MessageAddTask
import croniot.models.ParameterTask
import croniot.models.Result
import croniot.models.Task
import croniot.models.TaskState
import croniot.models.TaskStateInfo
import croniot.models.dto.TaskDto
import com.server.croniot.data.mappers.toDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.random.Random

class TaskService @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskTypeRepository: TaskTypeRepository,
    private val deviceRepository: DeviceRepository,
) {

    fun createTaskState(taskStateInfo: TaskStateInfo, taskId: Long) {
        taskRepository.createTaskState(taskStateInfo, taskId)
    }

    fun create(taskTypeId: Long, taskTypeUid: Long): Task? {
        return taskRepository.create(taskTypeId, taskTypeUid)
    }

    fun addTask(message: MessageAddTask): Result {
        try {
            val deviceUuid = message.deviceUuid
            val taskTypeUid = message.taskTypeUid
            val parametersValues = message.parametersValues

            val device = deviceRepository.getLazy(deviceUuid) ?: return Result(false, "")
            val deviceId = deviceRepository.getId(deviceUuid) ?: return Result(false, "")

            val taskTypeExists = taskTypeRepository.exists(
                taskTypeUid = taskTypeUid.toLong(),
                deviceId = deviceId
            )
            if (!taskTypeExists) return Result(false, "")

            val parametersValuesForDatabase = mutableMapOf<ParameterTask, String>()
            for ((parameterUid, value) in parametersValues) {
                val taskTypeId = taskTypeRepository.getId(deviceId = deviceId, taskTypeUid = taskTypeUid.toLong())
                    ?: continue

                val parameterTask = taskTypeRepository.getParameterTaskByUid(
                    parameterUid = parameterUid,
                    taskTypeId = taskTypeId
                )
                parameterTask?.let { parametersValuesForDatabase[it] = value }
            }

            val taskUid = Random.nextLong(1, 10000001) //TODO use UUID or DB sequence to avoid collisions
            val task = Task(taskUid, parametersValuesForDatabase, taskTypeUid.toLong())
            taskRepository.create(task)

            val taskStateInfo = TaskStateInfo(taskUid, ZonedDateTime.now(), TaskState.CREATED.name, 0.0, "")
            taskRepository.createState(task, taskStateInfo)

            val taskWithState = task.copy(mostRecentStateInfo = taskStateInfo)
            CoroutineScope(Dispatchers.IO).launch {
                MqttController.sendNewTask(deviceUuid, taskWithState)
                MqttController.sendTaskToDevice(deviceUuid, taskWithState)
            }

            return Result(true, "")
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(false, "")
        }
    }

    fun getTasksByDeviceUuid(deviceUuid: String): List<TaskDto> {
        val tasks = taskRepository.getAll(deviceUuid)
        return tasks.map { it.toDto() }
    }

    fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Result {
        // TODO validate device/task type existence
        CoroutineScope(Dispatchers.IO).launch {
            MqttController.requestTaskStateInfoSync(deviceUuid, taskTypeUid)
        }
        return Result(true, "")
    }
}
