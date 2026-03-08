package croniot.models.dto

import java.time.ZonedDateTime

data class SensorDataDto(
    val deviceUuid: String,
    val sensorTypeUid: Long,
    val value: String,
    val timestamp: ZonedDateTime,
)