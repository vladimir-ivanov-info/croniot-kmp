package croniot.models.dto

data class AccountDto(
    val uuid: String,
    val nickname: String,
    val email: String,
    val devices: List<DeviceDto>,
)