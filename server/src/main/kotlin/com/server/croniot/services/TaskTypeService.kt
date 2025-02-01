package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.TaskTypeRepository
import croniot.messages.MessageRegisterTaskType
import croniot.models.Device
import croniot.models.Result
import croniot.models.TaskType
import javax.inject.Inject

class TaskTypeService @Inject constructor(
    private val taskTypeRepository: TaskTypeRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    fun get(device: Device, taskTypeUid: Long): TaskType? {
        return taskTypeRepository.get(device, taskTypeUid)
    }

    fun exists(device: Device, taskTypeUid: Long): Boolean {
        return taskTypeRepository.exists(device, taskTypeUid)
    }

    fun registerTaskType(message: MessageRegisterTaskType): Result {
        var result: Result

        val deviceUuid = message.deviceUuid
        val deviceToken = message.deviceToken

        val device = deviceTokenRepository.getDeviceAssociatedWithToken(deviceToken)

        if (device != null && device.uuid == deviceUuid) {
            val taskType = message.taskType
            taskType.device = device

            for (parameter in taskType.parameters) {
                parameter.taskType = taskType
            }

            taskTypeRepository.create(taskType)
            result = Result(true, "Task ${taskType.uid} registered")
        } else {
            result = Result(false, "Incorrect device or token for task register process.")
        }

        return result
    }
}
