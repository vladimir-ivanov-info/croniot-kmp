package croniot.models

data class TaskProgressUpdate(
    val taskTypeUid: Long,
    val taskUid: Long,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)
