package croniot.models

data class ParameterSensor(
    val id: Long,
    val uid: Long,
    val name: String,
    val type: String,
    val unit: String,
    val description: String,
    val constraints: Map<String, String>,
)