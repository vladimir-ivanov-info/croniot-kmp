package croniot.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val uuid: String,
    val nickname: String,
    val email: String,
    val devices: List<DeviceDto>,
)
