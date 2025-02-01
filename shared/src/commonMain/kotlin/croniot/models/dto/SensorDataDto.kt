package croniot.models.dto

import java.time.ZonedDateTime

data class SensorDataDto(
    var deviceUuid: String,
    var sensorTypeUid: Long,
    var value: String,
    var timestamp: ZonedDateTime,
)
