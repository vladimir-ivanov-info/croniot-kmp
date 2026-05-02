package com.croniot.client.data.source.remote.ble

import Outcome
import com.croniot.client.data.source.remote.mqtt.TasksDataSource
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import croniot.messages.MessageAddTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BleTasksDataSourceImpl(
    private val appScope: CoroutineScope,
    private val connectionPool: BleConnectionPool,
) : TasksDataSource {

    private val newTaskJobs = ConcurrentHashMap<String, Job>()
    private val progressJobs = ConcurrentHashMap<String, Job>()

    override suspend fun listenTasks(deviceUuid: String, onNewTask: (Task) -> Unit) {
        val connection = connectionPool.get(deviceUuid) ?: return
        newTaskJobs.remove(deviceUuid)?.cancel()
        val job = appScope.launch {
            connection.observeNewTasks().collect { onNewTask(it) }
        }
        newTaskJobs[deviceUuid] = job
    }

    override suspend fun listenTaskStateInfos(
        deviceUuid: String,
        onNewEvent: (TaskStateInfoEvent) -> Unit,
    ) {
        val connection = connectionPool.get(deviceUuid) ?: return
        progressJobs.remove(deviceUuid)?.cancel()
        val job = appScope.launch {
            connection.observeTaskStateInfoEvents().collect { onNewEvent(it) }
        }
        progressJobs[deviceUuid] = job
    }

    override suspend fun stopListening(deviceUuid: String) {
        newTaskJobs.remove(deviceUuid)?.cancel()
        progressJobs.remove(deviceUuid)?.cancel()
    }

    override suspend fun stopAllListeners() {
        newTaskJobs.values.forEach { it.cancel() }
        newTaskJobs.clear()
        progressJobs.values.forEach { it.cancel() }
        progressJobs.clear()
    }

    // En modo BLE no hay backend persistente; las tareas vivas llegan vía notify.
    override suspend fun fetchTasks(deviceUuid: String): Outcome<List<Task>, TaskError> =
        Outcome.Ok(emptyList())

    override suspend fun sendNewTask(messageAddTask: MessageAddTask): Outcome<Unit, TaskError> {
        val connection = connectionPool.get(messageAddTask.deviceUuid)
            ?: return Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
        return when (connection.sendNewTask(messageAddTask)) {
            is Outcome.Ok -> Outcome.Ok(Unit)
            is Outcome.Err -> Outcome.Err(TaskError.Remote(RemoteError.Unknown))
        }
    }

    override suspend fun requestTaskStateInfoSync(
        deviceUuid: String,
        taskTypeUid: Long,
    ): Outcome<Unit, TaskError> {
        val connection = connectionPool.get(deviceUuid)
            ?: return Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
        return when (connection.requestTaskStateInfoSync(taskTypeUid)) {
            is Outcome.Ok -> Outcome.Ok(Unit)
            is Outcome.Err -> Outcome.Err(TaskError.Remote(RemoteError.Unknown))
        }
    }

    // El ESP32 no expone histórico persistente sobre BLE en MVP.
    override suspend fun fetchTaskStateInfoHistory(
        deviceUuid: String,
        limit: Int,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Outcome<List<TaskStateInfoHistoryEntry>, TaskError> = Outcome.Ok(emptyList())

    override suspend fun fetchTaskStateInfoHistoryCount(
        deviceUuid: String,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Outcome<Int, TaskError> = Outcome.Ok(0)
}
