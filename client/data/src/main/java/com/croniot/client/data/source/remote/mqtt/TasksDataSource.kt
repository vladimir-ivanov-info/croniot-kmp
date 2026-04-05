package com.croniot.client.data.source.remote.mqtt

import Outcome
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskHistoryFilter
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import com.croniot.client.domain.errors.TaskError
import croniot.messages.MessageAddTask

interface TasksDataSource {

    suspend fun listenTasks(
        deviceUuid: String,
        onNewTask: (Task) -> Unit,
    )

    suspend fun listenTaskStateInfos(
        deviceUuid: String,
        onNewEvent: (TaskStateInfoEvent) -> Unit,
    )

    suspend fun stopAllListeners()

    suspend fun fetchTasks(deviceUuid: String): Outcome<List<Task>, TaskError>

    suspend fun sendNewTask(messageAddTask: MessageAddTask): Outcome<Unit, TaskError>

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
