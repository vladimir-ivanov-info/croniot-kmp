package croniot.models.dto

data class TaskTypeDto(
    val uid: Long = 0,
    val name: String,
    val description: String,
    val parameters: List<ParameterTaskDto>,
)