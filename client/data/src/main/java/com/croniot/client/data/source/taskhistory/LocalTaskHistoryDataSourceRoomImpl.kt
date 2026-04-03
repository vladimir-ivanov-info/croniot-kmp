package com.croniot.client.data.source.taskhistory

import com.croniot.client.core.models.TaskStateInfoHistoryEntry
import com.croniot.client.data.source.local.database.daos.TaskHistoryCacheDao
import com.croniot.client.data.source.local.database.entities.TaskHistoryCacheEntity
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class LocalTaskHistoryDataSourceRoomImpl(
    private val taskHistoryCacheDao: TaskHistoryCacheDao,
) : LocalTaskHistoryDataSource {

    override suspend fun getPage(
        deviceUuid: String,
        limit: Int,
        before: String?,
        beforeId: Long?,
    ): List<TaskStateInfoHistoryEntry> {
        val beforeMillis = parseBeforeMillis(before)
        val effectiveBeforeId = beforeId ?: Long.MAX_VALUE
        return taskHistoryCacheDao
            .getPage(
                deviceUuid = deviceUuid,
                limit = limit,
                beforeMillis = beforeMillis,
                beforeId = effectiveBeforeId,
            )
            .map { it.toModel() }
    }

    override suspend fun savePage(deviceUuid: String, entries: List<TaskStateInfoHistoryEntry>) {
        if (entries.isEmpty()) return
        taskHistoryCacheDao.insertAll(entries.map { it.toEntity(deviceUuid) })
    }

    override suspend fun count(deviceUuid: String, before: String?, beforeId: Long?): Int {
        val beforeMillis = parseBeforeMillis(before)
        val effectiveBeforeId = beforeId ?: Long.MAX_VALUE
        return taskHistoryCacheDao.count(
            deviceUuid = deviceUuid,
            beforeMillis = beforeMillis,
            beforeId = effectiveBeforeId,
        )
    }
}

private fun parseBeforeMillis(before: String?): Long? {
    if (before.isNullOrBlank()) return null
    return before.toLongOrNull() ?: runCatching {
        OffsetDateTime.parse(before).toInstant().toEpochMilli()
    }.getOrNull()
}

private fun TaskStateInfoHistoryEntry.toEntity(deviceUuid: String): TaskHistoryCacheEntity =
    TaskHistoryCacheEntity(
        deviceUuid = deviceUuid,
        stateInfoId = stateInfoId.takeIf { it > 0L },
        taskUid = taskUid,
        taskTypeUid = taskTypeUid,
        timeStampMillis = dateTime.toInstant().toEpochMilli(),
        state = state,
        progress = progress,
        errorMessage = errorMessage,
    )

private fun TaskHistoryCacheEntity.toModel(): TaskStateInfoHistoryEntry =
    TaskStateInfoHistoryEntry(
        // For entries without server id, use a stable synthetic negative id based on local row id.
        stateInfoId = stateInfoId ?: -id,
        taskUid = taskUid,
        taskTypeUid = taskTypeUid,
        dateTime = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(timeStampMillis),
            ZoneOffset.UTC,
        ),
        state = state,
        progress = progress,
        errorMessage = errorMessage,
    )
