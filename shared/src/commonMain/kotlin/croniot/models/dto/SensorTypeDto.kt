package croniot.models.dto

data class SensorTypeDto(
    val uid: Long,
    val name: String,
    val description: String,
    val parameters: List<ParameterSensorDto>,
)