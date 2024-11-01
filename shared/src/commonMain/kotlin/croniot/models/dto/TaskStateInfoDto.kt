package croniot.models.dto

import croniot.models.TaskState
import java.time.ZonedDateTime

data class TaskStateInfoDto(
    var deviceUuid: String,
    var taskTypeUid: Long,
    var taskUid: Long,
    var dateTime: ZonedDateTime,
    var state: TaskState,
    var progress: Double,
    var errorMessage: String,
)
