package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.SensorTypeRepository
import croniot.messages.MessageRegisterSensorType
import croniot.models.Result
import javax.inject.Inject

class SensorTypeService @Inject constructor(
    private val sensorTypeRepository: SensorTypeRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val deviceRepository: DeviceRepository
) {

    fun registerSensorType(message: MessageRegisterSensorType): Result {
        val result: Result

        val deviceUuid = message.deviceUuid
        val deviceToken = message.deviceToken

        //val device = deviceTokenRepository.getDevice(deviceToken)

        val deviceExists = deviceRepository.isDeviceExists(deviceUuid)
        val tokenCorrect = deviceTokenRepository.isTokenCorrect(deviceUuid, deviceToken) //TODO move under if
        //if (device != null && device.uuid == deviceUuid) {
        if (deviceExists && tokenCorrect) {
            val sensor = message.sensorType
            //TODO sensor.device = device

           // for (parameter in sensor.parameters) {
                //TODO parameter.sensorType = sensor
           // }

            val deviceId = deviceRepository.getId(deviceUuid)
            if(deviceId != null){
                //sensorTypeRepository.
                sensorTypeRepository.upsert(sensor, deviceId) //TODO expect outcome and react to it

               /* for(parameter in sensor.parameters){
                    //parameterSensorRepository.upsert(parameter, sensorTypeId)
                    sensorTypeRepository.upsertParameter(parameter, sensorTypeId)
                }*/
                result = Result(true, "Sensor ${sensor.uid} registered")
            } else {
                result = Result(false, "Incorrect device or token for sensor register process.")
            }

            //result = Result(true, "Sensor ${sensor.uid} registered")
        } else {
            result = Result(false, "Incorrect device or token for sensor register process.")
        }

        return result
    }
}
