package croniot.models

data class TaskType(
    val uid: Long,
    val name: String,
    val description: String,
    val parameters: List<ParameterTask> = emptyList(),
)