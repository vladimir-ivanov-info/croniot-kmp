package com.croniot.client.data.source.remote

import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import kotlinx.coroutines.flow.Flow

interface TasksDataSource {
    fun observeTasks(deviceUuid: String): Flow<Task>

    suspend fun fetchTasks(deviceUuid: String): List<Task>

    fun observeTaskStateInfos(
        _deviceUuid: String,
    ): Flow<TaskStateInfo>
}
