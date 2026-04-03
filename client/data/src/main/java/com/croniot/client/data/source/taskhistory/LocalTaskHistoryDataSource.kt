package com.croniot.client.data.source.taskhistory

import com.croniot.client.core.models.TaskStateInfoHistoryEntry

interface LocalTaskHistoryDataSource {

    suspend fun getPage(
        deviceUuid: String,
        limit: Int,
        before: String? = null,
        beforeId: Long? = null,
    ): List<TaskStateInfoHistoryEntry>

    suspend fun savePage(
        deviceUuid: String,
        entries: List<TaskStateInfoHistoryEntry>,
    )

    suspend fun count(
        deviceUuid: String,
        before: String? = null,
        beforeId: Long? = null,
    ): Int
}
