package croniot.messages

import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(
    val email: String,
    val password: String,
    val deviceUuid: String,
    val deviceToken: String?,
    val deviceProperties: Map<String, String>,
)
