package com.croniot.client.domain.models.events

import com.croniot.client.domain.models.TaskStateInfo
import croniot.models.TaskKey

data class TaskStateInfoEvent(
    val key: TaskKey,
    val info: TaskStateInfo,
)
