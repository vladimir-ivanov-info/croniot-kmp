package croniot.models.dto

data class TaskDto(
    val uid: Long = 0,
    val taskTypeUid: Long,
    val parametersValues: Map<Long, String> = emptyMap(),
    val initialTaskStateInfo: TaskStateInfoDto? = null,
)