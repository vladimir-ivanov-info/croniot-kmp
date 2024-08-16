package croniot.models.dto

import croniot.models.Device
import croniot.models.ParameterSensor

data class SensorDto(
    var uid: Long,
    var name: String,
    var description: String,
    var parameters: MutableSet<ParameterSensorDto>,
)