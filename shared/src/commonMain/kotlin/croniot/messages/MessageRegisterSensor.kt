package croniot.messages

import croniot.models.Sensor

data class MessageRegisterSensor(val deviceUuid: String,
                                 val deviceToken: String,
                                 val sensor: Sensor)