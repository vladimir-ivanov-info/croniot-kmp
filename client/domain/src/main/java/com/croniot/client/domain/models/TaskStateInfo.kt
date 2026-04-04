package com.croniot.client.domain.models

import java.time.ZonedDateTime

data class TaskStateInfo(
    var dateTime: ZonedDateTime,
    var state: String,
    var progress: Double,
    var errorMessage: String,
)
