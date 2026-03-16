package croniot.messages

import kotlinx.serialization.Serializable

@Serializable
data class MessageRequestTaskStateInfoSync(
    val deviceUuid: String,
    val taskTypeUid: String,
)
