package croniot.models.dto

import java.time.ZonedDateTime

data class TaskStateInfoDto(
    val dateTime: ZonedDateTime,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)