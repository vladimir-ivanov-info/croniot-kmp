package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.SensorTypeRepository
import croniot.messages.MessageRegisterSensorType
import croniot.models.Result
import javax.inject.Inject

class SensorTypeService @Inject constructor(
    private val sensorTypeRepository: SensorTypeRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    fun registerSensorType(message: MessageRegisterSensorType): Result {
        val result: Result

        val deviceUuid = message.deviceUuid
        val deviceToken = message.deviceToken

        val device = deviceTokenRepository.getDeviceAssociatedWithToken(deviceToken)

        if (device != null && device.uuid == deviceUuid) {
            val sensor = message.sensorType
            sensor.device = device

            for (parameter in sensor.parameters) {
                parameter.sensorType = sensor
            }

            sensorTypeRepository.create(sensor)

            result = Result(true, "Sensor ${sensor.uid} registered")
        } else {
            result = Result(false, "Incorrect device or token for sensor register process.")
        }

        return result
    }
}
