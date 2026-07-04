package com.croniot.client.domain.models

import croniot.models.TaskKey

data class TaskStateInfoHistoryEntry(
    val stateInfoId: Long,
    val taskKey: TaskKey,
    val stateInfo: TaskStateInfo,
)
