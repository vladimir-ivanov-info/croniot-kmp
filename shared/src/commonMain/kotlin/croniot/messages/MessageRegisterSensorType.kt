package croniot.messages

import croniot.models.SensorType

data class MessageRegisterSensorType(val deviceUuid: String,
                                     val deviceToken: String,
                                     val sensorType: SensorType)