package croniot.messages

data class MessageLogin(
    val accountEmail: String = "",
    val accountPassword: String,
    val deviceUuid: String,
    val deviceToken: String?,
    val deviceProperties: Map<String, String>,
)
