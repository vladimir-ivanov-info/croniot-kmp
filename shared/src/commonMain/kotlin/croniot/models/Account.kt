package croniot.models

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val uuid: String,
    val nickname: String,
    val email: String,
    val devices: List<Device>,
)
