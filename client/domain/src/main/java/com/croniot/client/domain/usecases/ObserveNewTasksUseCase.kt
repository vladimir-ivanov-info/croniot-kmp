package com.croniot.client.domain.usecases

import com.croniot.client.domain.models.Task
import com.croniot.client.domain.repositories.TasksRepository
import kotlinx.coroutines.flow.Flow

class ObserveNewTasksUseCase(
    private val tasksRepository: TasksRepository,
) {
    operator fun invoke(deviceUuid: String): Flow<Task> =
        tasksRepository.observeNewTasks(deviceUuid)
}
