package com.croniot.client.core.mappers

import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto

fun TaskDto.toModel(): Task {
    return Task(
        uid = this.uid,
        taskTypeUid = this.taskTypeUid,
        parametersValues = this.parametersValues,
        initialTaskStateInfo = this.initialTaskStateInfo?.toModel(),
    )
}

fun TaskStateInfoDto.toModel(): TaskStateInfo {
    return TaskStateInfo(
        dateTime = this.dateTime,
        state = this.state,
        progress = this.progress,
        errorMessage = this.errorMessage,
    )
}
