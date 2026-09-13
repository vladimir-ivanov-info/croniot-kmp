package croniot.messages

import kotlinx.serialization.Serializable

@Serializable
data class MessageSensorData(
    val sensorTypeUid: Long,
    val value: String,
)
