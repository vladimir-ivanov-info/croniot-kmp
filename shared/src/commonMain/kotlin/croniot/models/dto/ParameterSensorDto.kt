package croniot.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ParameterSensorDto(
    val uid: Long,
    val name: String,
    val type: String,
    val unit: String,
    val description: String,
    val constraints: Map<String, String>,
)
