package croniot.models

import croniot.serialization.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class TaskStateInfo(
    val taskUid: Long,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val dateTime: ZonedDateTime,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)
