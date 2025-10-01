package com.croniot.client.core.models

import java.time.ZonedDateTime

data class TaskStateInfo(
    var deviceUuid: String,
    var taskTypeUid: Long,
    var taskUid: Long,
    var dateTime: ZonedDateTime,
    var state: String,
    var progress: Double,
    var errorMessage: String,
    // var messageSource: String
)
