package croniot.messages

data class MessageLoginRequest(
    val email: String,
    val password: String,
    val deviceUuid: String,
    val deviceToken: String?,
    val deviceProperties: Map<String, String>,
)
