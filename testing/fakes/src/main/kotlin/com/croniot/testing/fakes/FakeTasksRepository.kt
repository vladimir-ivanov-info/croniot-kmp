package com.croniot.testing.fakes

import Outcome
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import com.croniot.client.domain.repositories.TasksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeTasksRepository(
    private var fetchTasksOutcome: Outcome<List<Task>, TaskError> = Outcome.Ok(emptyList()),
    private var sendNewTaskOutcome: Outcome<Unit, TaskError> = Outcome.Ok(Unit),
    private var requestSyncOutcome: Outcome<Unit, TaskError> = Outcome.Ok(Unit),
    private var historyOutcome: Outcome<List<TaskStateInfoHistoryEntry>, TaskError> = Outcome.Ok(emptyList()),
    private var historyCountOutcome: Outcome<Int, TaskError> = Outcome.Ok(0),
    var latestTaskStateInfoEmittedByIoT: TaskStateInfo? = null,
    var latestTaskStateInfo: TaskStateInfo? = null,
    private var newTasksFlow: Flow<Task> = emptyFlow(),
    private var taskStateInfoEventsFlow: Flow<TaskStateInfoEvent> = emptyFlow(),
) : TasksRepository {

    var stopAllListenersCalls: Int = 0
        private set

    var stopListeningForInvocations: MutableList<String> = mutableListOf()
        private set

    var fetchTasksInvocations: MutableList<String> = mutableListOf()
        private set

    override fun getLatestTaskStateInfo(deviceUuid: String, taskTypeUid: Long): TaskStateInfo? = latestTaskStateInfo

    override fun getLatestTaskUidForTaskType(deviceUuid: String, taskTypeUid: Long): Long? = null

    override fun getLatestTaskStateInfoEmittedByIoT(deviceUuid: String, taskTypeUid: Long): TaskStateInfo? =
        latestTaskStateInfoEmittedByIoT

    override suspend fun fetchTasks(deviceUuid: String): Outcome<List<Task>, TaskError> {
        fetchTasksInvocations += deviceUuid
        return fetchTasksOutcome
    }

    override suspend fun listenTasks(deviceUuid: String) = Unit

    override suspend fun listenTaskStateInfos(deviceUuid: String) = Unit

    override suspend fun stopListeningFor(deviceUuid: String) {
        stopListeningForInvocations += deviceUuid
    }

    override suspend fun stopAllListeners() {
        stopAllListenersCalls++
    }

    override fun observeNewTasks(deviceUuid: String): Flow<Task> = newTasksFlow

    override fun observeTaskStateInfoUpdates(deviceUuid: String): Flow<TaskStateInfoEvent> = taskStateInfoEventsFlow

    override suspend fun sendNewTask(newTask: Task): Outcome<Unit, TaskError> = sendNewTaskOutcome

    override suspend fun addTask(task: Task) = Unit

    override suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Outcome<Unit, TaskError> =
        requestSyncOutcome

    override suspend fun fetchTaskStateInfoHistory(
        deviceUuid: String,
        limit: Int,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Outcome<List<TaskStateInfoHistoryEntry>, TaskError> = historyOutcome

    override suspend fun fetchTaskStateInfoHistoryCount(
        deviceUuid: String,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Outcome<Int, TaskError> = historyCountOutcome
}
