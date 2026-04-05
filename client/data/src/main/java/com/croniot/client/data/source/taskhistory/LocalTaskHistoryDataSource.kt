package com.croniot.client.data.source.taskhistory

import com.croniot.client.domain.models.TaskHistoryFilter
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry

interface LocalTaskHistoryDataSource {

    suspend fun getPage(
        deviceUuid: String,
        limit: Int,
        before: String? = null,
        beforeId: Long? = null,
        filter: TaskHistoryFilter = TaskHistoryFilter.NONE,
    ): List<TaskStateInfoHistoryEntry>

    suspend fun savePage(
        deviceUuid: String,
        entries: List<TaskStateInfoHistoryEntry>,
    )

    suspend fun count(
        deviceUuid: String,
        before: String? = null,
        beforeId: Long? = null,
        filter: TaskHistoryFilter = TaskHistoryFilter.NONE,
    ): Int
}
