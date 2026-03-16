package croniot.messages

import croniot.models.Account
import kotlinx.serialization.Serializable

@Serializable
data class MessageAccountInfo(val account: Account)
