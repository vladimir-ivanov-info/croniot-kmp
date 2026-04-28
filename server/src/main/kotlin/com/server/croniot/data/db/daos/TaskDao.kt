package com.server.croniot.data.db.daos

import croniot.models.Task
import croniot.models.dto.TaskStateInfoHistoryEntryDto

interface TaskDao {

    fun create(taskTypeId: Long, taskTypeUid: Long): Task?

    fun insert(task: Task): Long

    fun get(deviceUuid: String, taskTypeUid: Long, taskUid: Long): Task?

    fun getAll(deviceUuid: String): List<Task>

    fun getAllStateInfoHistory(
        deviceUuid: String,
        limit: Int,
        before: java.time.OffsetDateTime?,
        beforeId: Long?,
        taskTypeUid: Long? = null,
    ): List<TaskStateInfoHistoryEntryDto>

    fun getAllStateInfoHistoryCount(
        deviceUuid: String,
        before: java.time.OffsetDateTime?,
        beforeId: Long?,
        taskTypeUid: Long? = null,
    ): Int
}
