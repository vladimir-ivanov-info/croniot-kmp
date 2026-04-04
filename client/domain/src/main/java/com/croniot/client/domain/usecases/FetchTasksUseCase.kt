package com.croniot.client.domain.usecases

import Outcome
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.repositories.TasksRepository

class FetchTasksUseCase(
    private val tasksRepository: TasksRepository,
) {
    suspend operator fun invoke(deviceUuid: String): Outcome<List<Task>, TaskError> =
        tasksRepository.fetchTasks(deviceUuid)
}
