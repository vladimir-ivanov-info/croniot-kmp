package com.croniot.client.domain.repositories

import Outcome
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskHistoryFilter
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import com.croniot.client.domain.errors.TaskError
import kotlinx.coroutines.flow.Flow

interface TasksRepository {

    fun getLatestTaskStateInfo(deviceUuid: String, taskTypeUid: Long): TaskStateInfo?

    fun getLatestTaskUidForTaskType(deviceUuid: String, taskTypeUid: Long): Long?

    fun getLatestTaskStateInfoEmittedByIoT(deviceUuid: String, taskTypeUid: Long): TaskStateInfo?

    suspend fun fetchTasks(deviceUuid: String): Outcome<List<Task>, TaskError>

    suspend fun listenTasks(deviceUuid: String)

    suspend fun listenTaskStateInfos(deviceUuid: String)

    suspend fun stopAllListeners()

    fun observeNewTasks(deviceUuid: String): Flow<Task>

    fun observeTaskStateInfoUpdates(deviceUuid: String): Flow<TaskStateInfoEvent>

    suspend fun sendNewTask(newTask: Task): Outcome<Unit, TaskError>

    suspend fun addTask(task: Task)

    suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Outcome<Unit, TaskError>

    suspend fun fetchTaskStateInfoHistory(
        deviceUuid: String,
        limit: Int,
        before: String? = null,
        beforeId: Long? = null,
        filter: TaskHistoryFilter = TaskHistoryFilter.NONE,
    ): Outcome<List<TaskStateInfoHistoryEntry>, TaskError>

    suspend fun fetchTaskStateInfoHistoryCount(
        deviceUuid: String,
        before: String? = null,
        beforeId: Long? = null,
        filter: TaskHistoryFilter = TaskHistoryFilter.NONE,
    ): Outcome<Int, TaskError>
}
