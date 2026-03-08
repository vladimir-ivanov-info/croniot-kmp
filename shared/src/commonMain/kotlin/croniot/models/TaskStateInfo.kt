package croniot.models

import java.time.ZonedDateTime

data class TaskStateInfo(
    val taskUid: Long,
    val dateTime: ZonedDateTime,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)