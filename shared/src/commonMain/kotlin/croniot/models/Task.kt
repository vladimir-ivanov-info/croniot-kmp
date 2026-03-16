package croniot.models

import kotlinx.serialization.Serializable

@Serializable
data class TaskKey(
    val deviceUuid: String,
    val taskTypeUid: Long,
    val taskUid: Long,
)

@Serializable
data class Task(
    val uid: Long,
    val parametersValues: Map<ParameterTask, String>,
    val taskTypeUid: Long,
    val mostRecentStateInfo: TaskStateInfo? = null,
)
