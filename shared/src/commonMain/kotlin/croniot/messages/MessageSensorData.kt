package croniot.messages

import kotlinx.serialization.Serializable

@Serializable
data class MessageSensorData(
    val sensorTypeId: Long,
    val value: String,
)
