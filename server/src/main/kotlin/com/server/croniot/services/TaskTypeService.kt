package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.TaskTypeRepository
import croniot.messages.MessageRegisterTaskType
import croniot.models.Device
import croniot.models.Result
import croniot.models.TaskType
import javax.inject.Inject

class TaskTypeService @Inject constructor(
    private val taskTypeRepository: TaskTypeRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    fun getId(deviceId: Long, taskTypeUid: Long) : Long? {
        return taskTypeRepository.getId(deviceId, taskTypeUid)
    }

    fun get(device: Device, taskTypeUid: Long): TaskType? {
        return taskTypeRepository.get(device, taskTypeUid)
    }

    fun exists(deviceUid: String, taskTypeUid: Long): Boolean {
        val deviceId = deviceRepository.getId(deviceUid)
            ?: return false //TODO
        return taskTypeRepository.exists(
            deviceId = deviceId,
            taskTypeUid = taskTypeUid
        )
        //return taskTypeRepository.exists(device, taskTypeUid)
    }

    fun registerTaskType(message: MessageRegisterTaskType): Result {
        var result: Result

        val deviceUuid = message.deviceUuid
        val deviceToken = message.deviceToken

        val device = deviceTokenRepository.getDevice(deviceToken)
        //TODO change for: deviceExists and deviceTokenCorrect
        //val device = deviceTokenRepository.getDevice(deviceToken)

        if (device != null && device.uuid == deviceUuid) {
            val taskType = message.taskType
            //taskType.device = device

            /*for (parameter in taskType.parameters) {
                parameter.taskType = taskType
            }*/


            val deviceId = deviceRepository.getId(message.deviceUuid)
            if(deviceId != null){
                taskTypeRepository.insert(taskType, deviceId)
                result = Result(true, "Task ${taskType.uid} registered")
            } else {
                //TODO
                result = Result(false, "Incorrect device or token for task register process.")
            }


        } else {
            result = Result(false, "Incorrect device or token for task register process.")
        }

        return result
    }
}
