package croniot.models.dto

data class AccountDto(
    var uuid: String,
    var nickname: String,
    var email: String,
    var devices: /*Mutable*/List<DeviceDto>,
)
