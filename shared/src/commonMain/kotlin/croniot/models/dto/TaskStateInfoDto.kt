package croniot.models.dto

import java.time.ZonedDateTime

data class TaskStateInfoDto(
    var deviceUuid: String,
    var taskTypeUid: Long,
    var taskUid: Long,
    var dateTime: ZonedDateTime,
    var state: String,
    var progress: Double,
    var errorMessage: String,
)
