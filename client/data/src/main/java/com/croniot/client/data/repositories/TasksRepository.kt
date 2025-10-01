package com.croniot.client.data.repositories

import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import kotlinx.coroutines.flow.Flow

interface TasksRepository {

    fun getLatestTaskStateInfo(deviceUuid: String, taskTypeUid: Long): TaskStateInfo?

    fun getLatestTaskStateInfoEmittedByIoT(deviceUuid: String, taskTypeUid: Long): TaskStateInfo?

    suspend fun fetchTasks(deviceUuid: String): List<Task>

    fun listenTasks(deviceUuid: String)

    fun listenTaskStateInfos(deviceUuid: String)

    fun observeNewTasks(deviceUuid: String): Flow<Task>

    fun observeTaskStateInfoUpdates(deviceUuid: String): Flow<TaskStateInfo>
}
