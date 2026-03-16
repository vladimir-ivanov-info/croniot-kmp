package croniot.messages

import kotlinx.serialization.Serializable

@Serializable
data class MessageRegisterAccount(
    val accountUuid: String,
    val nickname: String,
    val email: String,
    val password: String,
)
