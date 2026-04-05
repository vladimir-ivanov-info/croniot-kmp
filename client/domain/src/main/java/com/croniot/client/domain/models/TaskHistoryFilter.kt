package com.croniot.client.domain.models

data class TaskHistoryFilter(
    val taskTypeUids: Set<Long> = emptySet(),
    val dateFromMillis: Long? = null,
    val dateToMillis: Long? = null,
) {
    val isActive: Boolean
        get() = taskTypeUids.isNotEmpty() || dateFromMillis != null || dateToMillis != null

    companion object {
        val NONE = TaskHistoryFilter()
    }
}