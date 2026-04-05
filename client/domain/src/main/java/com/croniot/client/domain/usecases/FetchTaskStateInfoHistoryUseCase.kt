package com.croniot.client.domain.usecases

import Outcome
import com.croniot.client.domain.models.TaskHistoryFilter
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.repositories.TasksRepository

class FetchTaskStateInfoHistoryUseCase(
    private val tasksRepository: TasksRepository,
) {
    suspend operator fun invoke(
        deviceUuid: String,
        limit: Int,
        before: String? = null,
        beforeId: Long? = null,
        filter: TaskHistoryFilter = TaskHistoryFilter.NONE,
    ): Outcome<List<TaskStateInfoHistoryEntry>, TaskError> =
        tasksRepository.fetchTaskStateInfoHistory(deviceUuid, limit, before, beforeId, filter)
}
