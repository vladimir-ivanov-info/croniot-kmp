import com.croniot.server.db.controllers.ControllerDb
import croniot.messages.MessageRegisterSensor

import croniot.models.Result

object RegisterSensorController {


    fun registerSensor(messageRegisterSensor: MessageRegisterSensor) : Result {

        var result = Result(false, "")

        val deviceUuid = messageRegisterSensor.deviceUuid
        val deviceToken = messageRegisterSensor.deviceToken

        //val deviceTokenCorrect = TokenController.isDeviceTokenCorrect(deviceUuid, deviceToken)
        val device = ControllerDb.deviceTokenDao.getDeviceAssociatedWithToken(deviceToken)

        if(device != null && device.uuid == deviceUuid){
            val sensor = messageRegisterSensor.sensor
            sensor.device = device


            for(parameter in sensor.parameters){
                parameter.sensor = sensor
            }

            ControllerDb.sensorDao.insert(sensor)
            result = Result(true, "Sensor ${sensor.uid} registered")
        } else {
            result = Result(false, "Incorrect device or token.")
        }

        return result
    }

}