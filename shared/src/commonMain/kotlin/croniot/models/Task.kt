package croniot.models

data class TaskKey(
    val deviceUuid: String,
    val taskTypeUid: Long,
    val taskUid: Long,
)

data class Task(
    val uid: Long,
    val parametersValues: Map<ParameterTask, String>,
    val taskTypeUid: Long,
    val mostRecentStateInfo: TaskStateInfo? = null,
)