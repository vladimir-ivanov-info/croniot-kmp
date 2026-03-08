package croniot.models

data class Account(
    val uuid: String,
    val nickname: String,
    val email: String,
    val devices: List<Device>,
)