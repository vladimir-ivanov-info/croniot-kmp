package croniot.messages

import croniot.models.SensorType
import kotlinx.serialization.Serializable

@Serializable
data class MessageRegisterSensorType(
    val deviceUuid: String,
    val deviceToken: String,
    val sensorType: SensorType,
)
