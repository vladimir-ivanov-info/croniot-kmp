package com.croniot.client.presentation.util

import com.croniot.client.domain.models.TaskStateInfo
import croniot.models.TaskState
import java.time.format.DateTimeFormatter

fun formatStateInfo(info: TaskStateInfo?): String {
    info ?: return ""
    val time = info.dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    val stateLabel = info.state.lowercase().replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
    return when (info.state) {
        TaskState.RUNNING.name -> "$stateLabel • ${info.progress.toInt()}% · $time"
        TaskState.ERROR.name -> "Error: ${info.errorMessage.take(50)} · $time"
        else -> "$stateLabel"
    }
}