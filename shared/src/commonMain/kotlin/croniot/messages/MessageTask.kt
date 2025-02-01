package croniot.messages

data class MessageTask(
    val taskTypeUid: Long,
    val parametersValues: Map<Long, String>,
    val taskUid: Long,
)
