package com.croniot.client.data.repositories

import Outcome
import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.core.models.events.TaskStateInfoEvent
import com.croniot.client.data.source.remote.mqtt.TasksDataSource
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.repositories.TasksRepository
import croniot.messages.MessageAddTask
import croniot.models.TaskKey
import croniot.models.TaskState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import onSuccess
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class TasksRepositoryImpl(
    private val tasksDataSource: TasksDataSource,
    private val appScope: CoroutineScope,
) : TasksRepository {

    private val tasksByDevice = ConcurrentHashMap<String, CopyOnWriteArrayList<Task>>()

    private val newTaskFlowByDevice = ConcurrentHashMap<String, MutableSharedFlow<Task>>()
    private val newTaskListenerJobs = ConcurrentHashMap<String, Job>()

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
    private val taskStateInfoListenerJobs = ConcurrentHashMap<String, Job>()
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

        return tasksDataSource.fetchTasks(deviceUuid)
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

    override fun listenTasks(deviceUuid: String) {
        synchronized(newTaskListenerJobs) {
            if (newTaskListenerJobs[deviceUuid]?.isActive == true) return
            val taskFlow = taskFlowFor(deviceUuid)

            newTaskListenerJobs[deviceUuid] = tasksDataSource.observeTasks(deviceUuid) // cold
                .onEach { task ->
                    tasksByDevice.getOrPut(deviceUuid) { CopyOnWriteArrayList() }.add(task)
                    taskFlow.tryEmit(task)

                    val key = TaskKey(deviceUuid = deviceUuid, taskUid = task.uid, taskTypeUid = task.taskTypeUid)
                    val initialState = task.initialTaskStateInfo ?: return@onEach
                    latestStateByTaskKey[key] = initialState
                    taskStateInfoFlowFor(deviceUuid).tryEmit(TaskStateInfoEvent(key = key, info = initialState))
                }
                .launchIn(appScope)
        }
    }

    override fun observeNewTasks(deviceUuid: String): Flow<Task> = taskFlowFor(deviceUuid)

    override fun observeTaskStateInfoUpdates(deviceUuid: String): Flow<TaskStateInfoEvent> =
        taskStateInfoFlowFor(deviceUuid)

    override fun listenTaskStateInfos(deviceUuid: String) {
        synchronized(taskStateInfoListenerJobs) {
            if (taskStateInfoListenerJobs[deviceUuid]?.isActive == true) return
            val shared = taskStateInfoFlowFor(deviceUuid)

            taskStateInfoListenerJobs[deviceUuid] = tasksDataSource.observeTaskStateInfos(deviceUuid)
                .onEach { event ->
                    latestStateByTaskKey[event.key] = event.info
                    shared.tryEmit(event)
                }
                .launchIn(appScope)
        }
    }

    override suspend fun stopAllListeners() {
        synchronized(newTaskListenerJobs) {
            newTaskListenerJobs.values.forEach { it.cancel() }
            newTaskListenerJobs.clear()
            newTaskFlowByDevice.clear()
        }
        synchronized(taskStateInfoListenerJobs) {
            taskStateInfoListenerJobs.values.forEach { it.cancel() }
            taskStateInfoListenerJobs.clear()
            taskStateInfoFlowByDevice.clear()
        }
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
        return tasksDataSource.sendNewTask(messageAddTask)
    }

    override suspend fun addTask(task: Task) {
        val stateInfo = task.initialTaskStateInfo ?: return
        val key = TaskKey(deviceUuid = task.deviceUuid, taskUid = task.uid, taskTypeUid = task.taskTypeUid)
        latestStateByTaskKey[key] = stateInfo
        taskStateInfoFlowFor(deviceUuid = task.deviceUuid).tryEmit(TaskStateInfoEvent(key = key, info = stateInfo))
    }

    override suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Outcome<Unit, TaskError> =
        tasksDataSource.requestTaskStateInfoSync(deviceUuid, taskTypeUid)
}
