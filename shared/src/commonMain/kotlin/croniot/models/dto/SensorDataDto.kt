package croniot.models.dto

import java.time.ZonedDateTime

data class SensorDataDto(
    var deviceUuid: String,
    var sensorTypeUid: Long,
    var value: String,  //TODO change to "sensorValue" or something, in order to not confuse with StateFlow's value
    var timestamp: ZonedDateTime,
)
