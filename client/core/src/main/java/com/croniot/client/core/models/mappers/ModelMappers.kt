package com.croniot.client.core.models.mappers

import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto


fun TaskDto.toModel() : Task {
    return Task(
        deviceUuid = this.deviceUuid,
        taskTypeUid = this.taskTypeUid,
        uid = this.uid,
        parametersValues = this.parametersValues,
        stateInfos = this.stateInfos.map { it.toModel() }.toMutableSet()
    )
}

fun TaskStateInfoDto.toModel() : TaskStateInfo {
    return TaskStateInfo(
        deviceUuid = this.deviceUuid,
        taskTypeUid = this.taskTypeUid,
        taskUid = this.taskUid,
        dateTime = this.dateTime,
        state = this.state,
        progress = this.progress,
        errorMessage = this.errorMessage
    )
}