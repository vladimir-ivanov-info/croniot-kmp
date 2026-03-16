package croniot.messages

import kotlinx.serialization.Serializable

@Serializable
data class MessageGetAccountInfo(/*val uuid: String,*/ val token: String)
