package croniot.models.dto

data class AccountDto (
    var uuid: String,
    var nickname: String,
    var email: String,
    var devices: MutableSet<DeviceDto>
)