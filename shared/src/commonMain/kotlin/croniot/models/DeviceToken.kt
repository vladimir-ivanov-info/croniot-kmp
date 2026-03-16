package croniot.models

import kotlinx.serialization.Serializable

@Serializable
data class DeviceToken(
    val deviceId: Long,
    val token: String,
)
