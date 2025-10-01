package croniot.messages

data class MessageRequestTaskStateInfoSync(
    val deviceUuid: String,
    val taskTypeUid: String,
)
