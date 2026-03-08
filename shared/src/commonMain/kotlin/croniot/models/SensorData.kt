package croniot.models

import java.time.ZonedDateTime

data class SensorData(
    val device: Device,
    val sensorType: SensorType,
    val value: String,
    val dateTime: ZonedDateTime,
)