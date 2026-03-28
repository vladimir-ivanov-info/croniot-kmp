package croniot.models.dto

import croniot.serialization.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class TaskStateInfoHistoryEntryDto(
    val taskUid: Long,
    val taskTypeUid: Long,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val dateTime: ZonedDateTime,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)
