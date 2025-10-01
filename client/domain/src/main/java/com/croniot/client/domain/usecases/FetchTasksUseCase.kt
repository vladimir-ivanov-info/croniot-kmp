package com.croniot.client.domain.usecases

import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.data.repositories.TasksRepository
import kotlinx.coroutines.flow.Flow

//TODO separate responsibilities
class FetchTasksUseCase(
    private val tasksRepository: TasksRepository
) {

    suspend operator fun invoke(deviceUuid: String): List<Task> {
        return tasksRepository.fetchTasks(deviceUuid)
    }

    fun observeNewTasks(deviceUuid: String): Flow<Task>{
        return tasksRepository.observeNewTasks(deviceUuid)
    }

    fun observeTaskStateInfoUpdates(deviceUuid: String) : Flow<TaskStateInfo>{
        return tasksRepository.observeTaskStateInfoUpdates(deviceUuid)
    }

    fun getLatestTaskStateInfo(deviceUuid: String, taskTypeUid: Long) : TaskStateInfo? {
        return tasksRepository.getLatestTaskStateInfo(deviceUuid, taskTypeUid)
    }
}