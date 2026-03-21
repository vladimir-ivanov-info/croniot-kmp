package croniot.models

import kotlinx.serialization.Serializable

@Serializable
data class ParameterSensor(
    val uid: Long,
    val name: String,
    val type: String,
    val unit: String,
    val description: String,
    val constraints: Map<String, String>,
)
