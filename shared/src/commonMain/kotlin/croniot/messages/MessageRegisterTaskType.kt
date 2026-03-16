package croniot.messages

import croniot.models.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class MessageRegisterTaskType(
    val deviceUuid: String,
    val deviceToken: String,
    val taskType: TaskType,
)
