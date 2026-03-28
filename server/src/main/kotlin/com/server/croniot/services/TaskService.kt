package com.server.croniot.services

import com.server.croniot.data.mappers.toDto
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
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.random.Random

class TaskService @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskTypeRepository: TaskTypeRepository,
    private val deviceRepository: DeviceRepository,
) {

    private val logger = KotlinLogging.logger {}

    val iotSendTimestamps = ConcurrentHashMap<String, Long>()

    fun createTaskState(taskStateInfo: TaskStateInfo, taskId: Long) {
        taskRepository.createTaskState(taskStateInfo, taskId)
    }

    fun create(taskTypeId: Long, taskTypeUid: Long): Task? {
        return taskRepository.create(taskTypeId, taskTypeUid)
    }

    fun addTask(message: MessageAddTask): Result {
        try {
            val t0 = System.currentTimeMillis()
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
            val t1 = System.currentTimeMillis()
            logger.debug { "addTask DB lookups: ${t1 - t0}ms" }

            val taskUid = Random.nextLong(1, 10000001) // TODO use UUID or DB sequence to avoid collisions
            val task = Task(taskUid, parametersValuesForDatabase, taskTypeUid.toLong())
            taskRepository.create(task)
            val t2 = System.currentTimeMillis()
            logger.debug { "addTask create task: ${t2 - t1}ms" }

            val taskStateInfo = TaskStateInfo(taskUid, ZonedDateTime.now(), TaskState.CREATED.name, 0.0, "")
            taskRepository.createState(task, taskStateInfo)
            val t3 = System.currentTimeMillis()
            logger.debug { "addTask createState: ${t3 - t2}ms" }

            val taskWithState = task.copy(mostRecentStateInfo = taskStateInfo)
            CoroutineScope(Dispatchers.IO).launch {
                val tMqtt0 = System.currentTimeMillis()
                MqttController.sendNewTask(deviceUuid, taskWithState)
                val tMqtt1 = System.currentTimeMillis()
                MqttController.sendTaskToDevice(deviceUuid, taskWithState)
                val tMqtt2 = System.currentTimeMillis()
                iotSendTimestamps["$deviceUuid:$taskTypeUid"] = tMqtt2
                logger.debug {
                    "addTask MQTT sendNewTask(→Android): ${tMqtt1 - tMqtt0}ms, sendTaskToDevice(→IoT): ${tMqtt2 - tMqtt1}ms"
                }
            }
            logger.debug { "addTask TOTAL (before MQTT): ${System.currentTimeMillis() - t0}ms" }

            return Result(true, "")
        } catch (e: Exception) {
            logger.error(e) { "Error in addTask" }
            return Result(false, "")
        }
    }

    fun getTasksByDeviceUuid(deviceUuid: String): List<TaskDto> {
        val tasks = taskRepository.getAll(deviceUuid)
        return tasks.map { it.toDto() }
    }

    fun getTaskStateInfoHistory(deviceUuid: String): List<TaskStateInfoHistoryEntryDto> {
        return taskRepository.getAllStateInfoHistory(deviceUuid)
    }

    fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Result {
        // TODO validate device/task type existence
        CoroutineScope(Dispatchers.IO).launch {
            MqttController.requestTaskStateInfoSync(deviceUuid, taskTypeUid)
        }
        return Result(true, "")
    }
}
