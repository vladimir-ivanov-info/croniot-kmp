package croniot.models

import kotlinx.serialization.Serializable

@Serializable
data class TaskType(
    val uid: Long,
    val name: String,
    val description: String,
    val parameters: List<ParameterTask> = emptyList(),
)
