package com.croniot.client.core.models.events

import com.croniot.client.core.models.TaskStateInfo
import croniot.models.TaskKey

data class TaskStateInfoEvent(
    val key: TaskKey,
    val info: TaskStateInfo
)