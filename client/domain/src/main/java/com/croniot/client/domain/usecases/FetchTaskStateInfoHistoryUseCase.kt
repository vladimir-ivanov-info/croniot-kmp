package com.croniot.client.domain.usecases

import Outcome
import com.croniot.client.core.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.repositories.TasksRepository

class FetchTaskStateInfoHistoryUseCase(
    private val tasksRepository: TasksRepository,
) {
    suspend operator fun invoke(deviceUuid: String, limit: Int, offset: Int): Outcome<List<TaskStateInfoHistoryEntry>, TaskError> =
        tasksRepository.fetchTaskStateInfoHistory(deviceUuid, limit, offset)
}
