package com.croniot.client.data.repositories

import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.data.source.remote.TasksDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap

class TasksRepositoryImpl(
    private val tasksDataSource: TasksDataSource,
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : TasksRepository {

    private val tasks = mutableMapOf<String, MutableList<Task>>()
    private val taskJobs = mutableMapOf<String, Job>()

    private val perDevice = ConcurrentHashMap<String, MutableSharedFlow<Task>>()
    private fun bus(device: String) = perDevice.computeIfAbsent(device) {
        MutableSharedFlow(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    override suspend fun fetchTasks(deviceUuid: String): List<Task> {
        var cachedTaskList = tasks[deviceUuid]

        if (cachedTaskList.isNullOrEmpty()) {
            val fetchedTasks = tasksDataSource.fetchTasks(deviceUuid)
            // TODO mutex
            tasks.getOrPut(deviceUuid) { mutableListOf() }.addAll(fetchedTasks)

            cachedTaskList = tasks.getOrPut(deviceUuid) { mutableListOf() }
        }
        return cachedTaskList.toList()
    }

    override fun listenTasks(deviceUuid: String) {
        if (taskJobs[deviceUuid]?.isActive == true) return
        val shared = bus(deviceUuid)

        taskJobs[deviceUuid] = tasksDataSource.observeTasks(deviceUuid) // cold
            .onEach { task ->
                tasks.getOrPut(deviceUuid) { mutableListOf() }.add(task)
                shared.tryEmit(task)

                // task update taskstateinfos
                val taskStateInfos = task.stateInfos
                val latestTaskStateInfo = taskStateInfos.maxBy { it.dateTime }
                busTaskStateInfo(deviceUuid).tryEmit(latestTaskStateInfo)
            }
            .launchIn(appScope)
    }

    override fun observeNewTasks(deviceUuid: String): Flow<Task> = bus(deviceUuid) // hot

    private val taskStateInfoJobs = mutableMapOf<String, Job>()
    private val perDeviceTaskStateInfos = ConcurrentHashMap<String, MutableSharedFlow<TaskStateInfo>>()
    private fun busTaskStateInfo(device: String) = perDeviceTaskStateInfos.computeIfAbsent(device) {
        MutableSharedFlow(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    override fun observeTaskStateInfoUpdates(deviceUuid: String): Flow<TaskStateInfo> = busTaskStateInfo(deviceUuid)

    override fun listenTaskStateInfos(deviceUuid: String) {
        if (taskStateInfoJobs[deviceUuid]?.isActive == true) return
        val shared = busTaskStateInfo(deviceUuid)

        taskStateInfoJobs[deviceUuid] = tasksDataSource.observeTaskStateInfos(deviceUuid) // cold
            .onEach { taskStateInfo ->
                // update corresponding task
                val taskList = tasks[deviceUuid]
                if (taskList != null) {
                    val foundTask = taskList.find { it.uid == taskStateInfo.taskUid }

                    if (foundTask != null) {
                        foundTask.stateInfos.add(taskStateInfo)
                    } else {
                    }
                } else {
                    // TODO
                }

                println("${taskStateInfo.state}")
                shared.tryEmit(taskStateInfo)
            }
            .launchIn(appScope)
    }

    override fun getLatestTaskStateInfo(deviceUuid: String, taskTypeUid: Long): TaskStateInfo? {
        var result: TaskStateInfo? = null

        val taskList = tasks[deviceUuid]
        if (taskList != null) {
            val filteredTasks = taskList.filter {
                it.deviceUuid == deviceUuid && it.taskTypeUid == taskTypeUid
            }

            if (filteredTasks.isNotEmpty()) {
                val latestTask = filteredTasks.maxByOrNull { task ->
                    task.getMostRecentState().dateTime
                }

                if (latestTask != null) {
                    result = latestTask.getMostRecentState()
                }
            }
        } else {
            // TODO
        }

        return result
    }

    // TODO ConcurrentModificationException when moving slider too fast
    override fun getLatestTaskStateInfoEmittedByIoT(deviceUuid: String, taskTypeUid: Long): TaskStateInfo? {
        var result: TaskStateInfo? = null

        val taskList = tasks[deviceUuid]
        if (taskList != null) {
            val filteredTasks = taskList.filter {
                it.deviceUuid == deviceUuid && it.taskTypeUid == taskTypeUid
            }

            if (filteredTasks.isNotEmpty()) {
                val latestTask = filteredTasks.maxByOrNull { task ->
                    task.getMostRecentState().dateTime
                }

                if (latestTask != null) {
                    result = latestTask.getMostRecentStateEmittedByIoT()
                }
            }
        } else {
            // TODO
        }

        return result
    }
}
