import com.croniot.server.db.controllers.ControllerDb
import croniot.messages.MessageRegisterSensorType

import croniot.models.Result

object RegisterSensorController {


    fun registerSensor(messageRegisterSensorType: MessageRegisterSensorType) : Result {

        var result = Result(false, "")

        val deviceUuid = messageRegisterSensorType.deviceUuid
        val deviceToken = messageRegisterSensorType.deviceToken

        //val deviceTokenCorrect = TokenController.isDeviceTokenCorrect(deviceUuid, deviceToken)
        val device = ControllerDb.deviceTokenDao.getDeviceAssociatedWithToken(deviceToken)

        if(device != null && device.uuid == deviceUuid){
            val sensor = messageRegisterSensorType.sensorType
            sensor.device = device


            for(parameter in sensor.parameters){
                parameter.sensorType = sensor
            }

            ControllerDb.sensorDao.insert(sensor)
            result = Result(true, "Sensor ${sensor.uid} registered")
        } else {
            result = Result(false, "Incorrect device or token for sensor register process.")
        }

        return result
    }

}