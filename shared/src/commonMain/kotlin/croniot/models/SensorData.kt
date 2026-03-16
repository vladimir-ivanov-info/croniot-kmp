package croniot.models

import croniot.serialization.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class SensorData(
    val device: Device,
    val sensorType: SensorType,
    val value: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val dateTime: ZonedDateTime,
)
