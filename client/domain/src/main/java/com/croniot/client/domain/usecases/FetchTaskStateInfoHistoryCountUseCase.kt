package com.croniot.client.domain.usecases

import Outcome
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.repositories.TasksRepository

class FetchTaskStateInfoHistoryCountUseCase(
    private val tasksRepository: TasksRepository,
) {
    suspend operator fun invoke(
        deviceUuid: String,
        before: String? = null,
        beforeId: Long? = null,
    ): Outcome<Int, TaskError> =
        tasksRepository.fetchTaskStateInfoHistoryCount(deviceUuid, before, beforeId)
}
