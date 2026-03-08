package croniot.models.dto

data class ParameterTaskDto(
    val uid: Long,
    val name: String,
    val type: String,
    val unit: String,
    val description: String,
    val constraints: Map<String, String>,
)