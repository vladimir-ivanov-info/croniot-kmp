package croniot.messages

import kotlinx.serialization.Serializable

@Serializable
data class MessageTask(
    val taskTypeUid: Long,
    val parametersValues: Map<Long, String>,
    val taskUid: Long,
)
