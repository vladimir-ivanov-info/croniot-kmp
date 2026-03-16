package croniot.models

import kotlinx.serialization.Serializable

@Serializable
data class TaskProgressUpdate(
    val taskTypeUid: Long,
    val taskUid: Long,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)
