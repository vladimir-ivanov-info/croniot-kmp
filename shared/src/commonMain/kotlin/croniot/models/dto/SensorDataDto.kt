package croniot.models.dto

import croniot.serialization.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class SensorDataDto(
    val deviceUuid: String,
    val sensorTypeUid: Long,
    val value: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val timestamp: ZonedDateTime,
)
