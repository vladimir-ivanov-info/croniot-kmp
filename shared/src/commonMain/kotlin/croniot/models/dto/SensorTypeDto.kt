package croniot.models.dto

data class SensorTypeDto(
    var uid: Long,
    var name: String,
    var description: String,
    var parameters: MutableSet<ParameterSensorDto>,
)
