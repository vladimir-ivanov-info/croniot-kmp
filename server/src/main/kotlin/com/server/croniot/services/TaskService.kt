package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.TaskRepository
import com.server.croniot.data.repositories.TaskTypeRepository
import com.server.croniot.mqtt.MqttController
import croniot.messages.MessageAddTask
import croniot.models.Device
import croniot.models.ParameterTask
import croniot.models.Result
import croniot.models.Task
import croniot.models.TaskState
import croniot.models.TaskStateInfo
import croniot.models.TaskType
import croniot.models.dto.TaskDto
import croniot.models.toDto
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

    fun getLazy(deviceUuid: String, taskTypeUid: Long, taskUid: Long): Task? {
        return taskRepository.getLazy(deviceUuid, taskTypeUid, taskUid)
    }

    fun createTaskState(taskStateInfo: TaskStateInfo) {
        taskRepository.createTaskState(taskStateInfo)
    }

    fun create(device: Device, taskType: TaskType): Task {
        return taskRepository.create(device, taskType)
    }

    fun addTask(message: MessageAddTask): Result {
        var result = Result(false, "")
        try {
            val deviceUuid = message.deviceUuid
            val taskTypeUid = message.taskTypeUid
            val parametersValues = message.parametersValues

            val device = deviceRepository.getLazy(deviceUuid) // 2482 ms -> 1089 ms without logback -> 32 ms with val query = sess.createQuery(cr).uniqueResultOptional()
            device?.let {
                val taskType = taskTypeRepository.getLazy(device, taskTypeUid.toLong()) // 53-129 ms ->  2-11ms

                taskType?.let {
                    val parametersValuesForDatabase = mutableMapOf<ParameterTask, String>()
                    for (parameterValueEntry in parametersValues) {
                        val parameterUid = parameterValueEntry.key
                        val value = parameterValueEntry.value

                        val parameterTask = taskTypeRepository.getParameterTaskByUid(parameterUid, taskType)

                        parameterTask?.let {
                            parametersValuesForDatabase[parameterTask] = value
                        }
                    }

                    val taskUid = Random.nextLong(1, 10000001) // TODO -> duplicate task in TaskDaoImpl
                    val task = Task(taskUid, parametersValuesForDatabase, taskType, mutableSetOf())
                    taskRepository.create(task) // 7-47 ms

                    val taskStateInfo = TaskStateInfo(ZonedDateTime.now(), TaskState.CREATED, 0.0, "", task) // TODO check if taskConfiguration.id gets updated from 0 to actual value
                    taskRepository.createState(taskStateInfo)

                    CoroutineScope(Dispatchers.IO).launch {
                        MqttController.sendNewTask(deviceUuid, task, taskStateInfo)
                        MqttController.sendTaskToDevice(deviceUuid, task) // 468-620 ms  ->  99-616 ms
                    }

                    result = Result(true, "")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result = Result(false, "")
        }

        return result
    }

    fun getTasksByDeviceUuid(deviceUuid: String): List<TaskDto> {
        val tasks = taskRepository.getAll(deviceUuid)
        val tasksDto = tasks.map { it.toDto() }.toMutableList()
        return tasksDto
    }
}
