package com.croniot.client.core.models

import java.time.ZonedDateTime

data class TaskStateInfoHistoryEntry(
    val taskUid: Long,
    val taskTypeUid: Long,
    val dateTime: ZonedDateTime,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)
