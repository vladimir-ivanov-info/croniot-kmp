package com.croniot.client.domain.models

data class Task(
    var deviceUuid: String = "",
    var taskTypeUid: Long = 0,
    var uid: Long = 0,
    var parametersValues: Map<Long, String> = mutableMapOf(),
    var initialTaskStateInfo: TaskStateInfo? = null,
)
