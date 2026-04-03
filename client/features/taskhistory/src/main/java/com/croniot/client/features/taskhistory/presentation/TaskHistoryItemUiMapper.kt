package com.croniot.client.features.taskhistory.presentation

import com.croniot.client.core.util.DateTimeUtil
import croniot.models.TaskState
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val taskHistoryTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun buildTaskHistoryItem(
    stateInfoId: Long,
    taskUid: Long,
    taskTypeUid: Long,
    taskTypeName: String,
    dateTime: ZonedDateTime,
    state: String,
    progress: Double,
    errorMessage: String,
): TaskHistoryItem {
    val stateLabel = state.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    val subtitle = when (state) {
        TaskState.RUNNING.name -> "$stateLabel - ${progress.toInt()}%"
        TaskState.ERROR.name -> "Error: ${errorMessage.take(50)}"
        else -> stateLabel
    }

    return TaskHistoryItem(
        stateInfoId = stateInfoId,
        taskUid = taskUid,
        taskTypeUid = taskTypeUid,
        taskTypeName = taskTypeName,
        dateTime = dateTime,
        state = state,
        progress = progress,
        errorMessage = errorMessage,
        subtitle = subtitle,
        relativeTime = DateTimeUtil.formatRelativeTime(dateTime),
        time = dateTime.format(taskHistoryTimeFormatter),
    )
}
