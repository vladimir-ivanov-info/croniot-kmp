package croniot.messages

import croniot.models.TaskType

data class MessageRegisterTaskType(
    val deviceUuid: String,
    val deviceToken: String,
    val taskType: TaskType,
)
