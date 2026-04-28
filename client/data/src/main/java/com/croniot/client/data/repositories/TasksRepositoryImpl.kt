package com.croniot.client.data.repositories

import Outcome
import com.croniot.client.data.source.remote.mqtt.TasksDataSource
import com.croniot.client.data.source.taskhistory.LocalTaskHistoryDataSource
import com.croniot.client.data.source.transport.TransportRouter
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.models.TransportKind
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import com.croniot.client.domain.repositories.TasksRepository
import croniot.messages.MessageAddTask
import croniot.models.TaskKey
import croniot.models.TaskState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import onSuccess
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class TasksRepositoryImpl(
    private val cloudTasksDataSource: TasksDataSource,
    private val bleTasksDataSource: TasksDataSource,
    private val transportRouter: TransportRouter,
    private val localTaskHistoryDataSource: LocalTaskHistoryDataSource,
) : TasksRepository {
    //TODO Pager3
    private val tasksByDevice = ConcurrentHashMap<String, CopyOnWriteArrayList<Task>>()

    private val newTaskFlowByDevice = ConcurrentHashMap<String, MutableSharedFlow<Task>>()

    private fun dataSourceFor(deviceUuid: String): TasksDataSource =
        when (transportRouter.transportFor(deviceUuid)) {
            TransportKind.BLE -> bleTasksDataSource
            TransportKind.CLOUD -> cloudTasksDataSource
        }

    private fun taskFlowFor(deviceUuid: String): MutableSharedFlow<Task> {
        return newTaskFlowByDevice.getOrPut(deviceUuid) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
    }

    private val taskStateInfoFlowByDevice = ConcurrentHashMap<String, MutableSharedFlow<TaskStateInfoEvent>>()
    private val latestStateByTaskKey = ConcurrentHashMap<TaskKey, TaskStateInfo>()

    private fun taskStateInfoFlowFor(deviceUuid: String): MutableSharedFlow<TaskStateInfoEvent> {
        return taskStateInfoFlowByDevice.getOrPut(deviceUuid) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
    }

    override suspend fun fetchTasks(deviceUuid: String): Outcome<List<Task>, TaskError> {
        val cachedTaskList = tasksByDevice[deviceUuid]
        if (!cachedTaskList.isNullOrEmpty()) return Outcome.Ok(cachedTaskList.toList())

        return dataSourceFor(deviceUuid).fetchTasks(deviceUuid)
            .onSuccess { fetchedTasks ->
                fetchedTasks.forEach { task ->
                    task.initialTaskStateInfo?.let { initialState ->
                        val key = TaskKey(deviceUuid = deviceUuid, taskUid = task.uid, taskTypeUid = task.taskTypeUid)
                        latestStateByTaskKey[key] = initialState
                    }
                }
                val list = tasksByDevice.getOrPut(deviceUuid) { CopyOnWriteArrayList() }
                list.addAll(fetchedTasks)
            }
    }

    override suspend fun listenTasks(deviceUuid: String) {
        val taskFlow = taskFlowFor(deviceUuid)
        dataSourceFor(deviceUuid).listenTasks(deviceUuid) { task ->
            tasksByDevice.getOrPut(deviceUuid) { CopyOnWriteArrayList() }.add(task)
            taskFlow.tryEmit(task)

            val key = TaskKey(deviceUuid = deviceUuid, taskUid = task.uid, taskTypeUid = task.taskTypeUid)
            val initialState = task.initialTaskStateInfo ?: return@listenTasks
            latestStateByTaskKey[key] = initialState
            taskStateInfoFlowFor(deviceUuid).tryEmit(TaskStateInfoEvent(key = key, info = initialState))
        }
    }

    override fun observeNewTasks(deviceUuid: String): Flow<Task> = taskFlowFor(deviceUuid)

    override fun observeTaskStateInfoUpdates(deviceUuid: String): Flow<TaskStateInfoEvent> =
        taskStateInfoFlowFor(deviceUuid)

    override suspend fun listenTaskStateInfos(deviceUuid: String) {
        val shared = taskStateInfoFlowFor(deviceUuid)
        dataSourceFor(deviceUuid).listenTaskStateInfos(deviceUuid) { event ->
            latestStateByTaskKey[event.key] = event.info
            shared.tryEmit(event)
        }
    }

    override suspend fun stopListeningFor(deviceUuid: String) {
        dataSourceFor(deviceUuid).stopListening(deviceUuid)
        newTaskFlowByDevice.remove(deviceUuid)
        taskStateInfoFlowByDevice.remove(deviceUuid)
        tasksByDevice.remove(deviceUuid)
        latestStateByTaskKey.keys
            .filter { it.deviceUuid == deviceUuid }
            .forEach { latestStateByTaskKey.remove(it) }
    }

    override suspend fun stopAllListeners() {
        cloudTasksDataSource.stopAllListeners()
        bleTasksDataSource.stopAllListeners()
        newTaskFlowByDevice.clear()
        taskStateInfoFlowByDevice.clear()
        tasksByDevice.clear()
        latestStateByTaskKey.clear()
    }

    override fun getLatestTaskUidForTaskType(deviceUuid: String, taskTypeUid: Long): Long? {
        return tasksByDevice[deviceUuid]
            ?.filter { it.taskTypeUid == taskTypeUid }
            ?.maxByOrNull { it.uid }
            ?.uid
    }

    override fun getLatestTaskStateInfo(deviceUuid: String, taskTypeUid: Long): TaskStateInfo? {
        return latestStateByTaskKey
            .filterKeys { it.deviceUuid == deviceUuid && it.taskTypeUid == taskTypeUid }
            .values
            .maxByOrNull { it.dateTime }
    }

    override fun getLatestTaskStateInfoEmittedByIoT(deviceUuid: String, taskTypeUid: Long): TaskStateInfo? {
        return latestStateByTaskKey
            .filterKeys { it.deviceUuid == deviceUuid && it.taskTypeUid == taskTypeUid }
            .values
            .filter {
                it.state != TaskState.CREATED.name &&
                    it.state != TaskState.UNDEFINED.name &&
                    it.state != TaskState.ERROR.name
            }
            .maxByOrNull { it.dateTime }
    }

    override suspend fun sendNewTask(newTask: Task): Outcome<Unit, TaskError> {
        val messageAddTask = MessageAddTask( // TODO refactor MessageAddTask so it can receive Task object directly
            newTask.deviceUuid,
            newTask.taskTypeUid.toString(),
            newTask.parametersValues
        )
        taskFlowFor(newTask.deviceUuid).tryEmit(newTask) // TODO add TaskStateInfo como "SENT_TO_SERVER"
        return dataSourceFor(newTask.deviceUuid).sendNewTask(messageAddTask)
    }

    override suspend fun addTask(task: Task) {
        val stateInfo = task.initialTaskStateInfo ?: return
        val key = TaskKey(deviceUuid = task.deviceUuid, taskUid = task.uid, taskTypeUid = task.taskTypeUid)
        latestStateByTaskKey[key] = stateInfo
        taskStateInfoFlowFor(deviceUuid = task.deviceUuid).tryEmit(TaskStateInfoEvent(key = key, info = stateInfo))
    }

    override suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Outcome<Unit, TaskError> =
        dataSourceFor(deviceUuid).requestTaskStateInfoSync(deviceUuid, taskTypeUid)

    override suspend fun fetchTaskStateInfoHistory(
        deviceUuid: String,
        limit: Int,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Outcome<List<TaskStateInfoHistoryEntry>, TaskError> {
        // When a filter is active, skip local cache (it stores unfiltered data) and go directly to the server.
        if (taskTypeUid != null) {
            return dataSourceFor(deviceUuid).fetchTaskStateInfoHistory(deviceUuid, limit, before, beforeId, taskTypeUid)
        }

        val localPage = localTaskHistoryDataSource.getPage(
            deviceUuid = deviceUuid,
            limit = limit,
            before = before,
            beforeId = beforeId,
        )
        if (localPage.size >= limit) return Outcome.Ok(localPage)

        val remoteBeforeId = beforeId?.takeIf { it > 0L }
        val remoteResult = dataSourceFor(deviceUuid)
            .fetchTaskStateInfoHistory(deviceUuid, limit, before, remoteBeforeId, taskTypeUid)
        return when (remoteResult) {
            is Outcome.Ok -> {
                if (remoteResult.value.isNotEmpty()) {
                    localTaskHistoryDataSource.savePage(deviceUuid, remoteResult.value)
                }
                val mergedLocal = localTaskHistoryDataSource.getPage(
                    deviceUuid = deviceUuid,
                    limit = limit,
                    before = before,
                    beforeId = beforeId,
                )
                if (mergedLocal.isNotEmpty()) Outcome.Ok(mergedLocal) else Outcome.Ok(remoteResult.value)
            }

            is Outcome.Err -> {
                if (localPage.isNotEmpty()) Outcome.Ok(localPage) else Outcome.Err(remoteResult.error)
            }
        }
    }

    override suspend fun fetchTaskStateInfoHistoryCount(
        deviceUuid: String,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Outcome<Int, TaskError> {
        if (taskTypeUid != null) {
            return dataSourceFor(deviceUuid).fetchTaskStateInfoHistoryCount(deviceUuid, before, beforeId, taskTypeUid)
        }

        val localCount = localTaskHistoryDataSource.count(
            deviceUuid = deviceUuid,
            before = before,
            beforeId = beforeId,
        )

        val remoteBeforeId = beforeId?.takeIf { it > 0L }
        val remoteResult = dataSourceFor(deviceUuid)
            .fetchTaskStateInfoHistoryCount(deviceUuid, before, remoteBeforeId, taskTypeUid)
        return when (remoteResult) {
            is Outcome.Ok -> Outcome.Ok(maxOf(localCount, remoteResult.value))
            is Outcome.Err -> if (localCount > 0) Outcome.Ok(localCount) else Outcome.Err(remoteResult.error)
        }
    }
}
