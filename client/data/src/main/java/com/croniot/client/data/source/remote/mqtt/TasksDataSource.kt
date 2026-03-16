package com.croniot.client.data.source.remote.mqtt

import Outcome
import com.croniot.client.core.models.Task
import com.croniot.client.core.models.events.TaskStateInfoEvent
import com.croniot.client.domain.errors.TaskError
import croniot.messages.MessageAddTask
import kotlinx.coroutines.flow.Flow

interface TasksDataSource {

    fun observeTasks(deviceUuid: String): Flow<Task>

    suspend fun fetchTasks(deviceUuid: String): Outcome<List<Task>, TaskError>

    fun observeTaskStateInfos(_deviceUuid: String): Flow<TaskStateInfoEvent>

    suspend fun sendNewTask(messageAddTask: MessageAddTask): Outcome<Unit, TaskError>

    suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Outcome<Unit, TaskError>
}
