package croniot.models.dto

import croniot.models.TaskState
import java.time.ZonedDateTime

data class TaskStateInfoDto(
    var dateTime: ZonedDateTime,
    var state: TaskState,
    var errorMessage: String,
)
