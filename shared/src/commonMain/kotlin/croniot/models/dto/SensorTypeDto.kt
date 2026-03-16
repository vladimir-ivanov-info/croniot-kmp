package croniot.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class SensorTypeDto(
    val uid: Long,
    val name: String,
    val description: String,
    val parameters: List<ParameterSensorDto>,
)
