package croniot.models

import kotlinx.serialization.Serializable

@Serializable
data class SensorType(
    val uid: Long,
    val name: String,
    val description: String,
    val parameters: List<ParameterSensor>,
)
