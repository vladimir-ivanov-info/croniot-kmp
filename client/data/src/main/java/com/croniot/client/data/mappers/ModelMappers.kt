package com.croniot.client.data.mappers

import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import croniot.models.TaskKey
import croniot.models.dto.TaskStateInfoHistoryEntryDto

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

fun TaskStateInfoHistoryEntryDto.toModel(deviceUuid: String): TaskStateInfoHistoryEntry {
    return TaskStateInfoHistoryEntry(
        stateInfoId = this.stateInfoId,
        taskKey = TaskKey(
            deviceUuid = deviceUuid,
            taskTypeUid = this.taskTypeUid,
            taskUid = this.taskUid,
        ),
        stateInfo = TaskStateInfo(
            dateTime = this.dateTime,
            state = this.state,
            progress = this.progress,
            errorMessage = this.errorMessage,
        ),
    )
}
