package com.croniot.client.domain.usecases

import Outcome
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.repositories.TasksRepository

class RequestTaskStateInfoSyncUseCaseImpl(
    private val tasksRepository: TasksRepository,
) : RequestTaskStateInfoSyncUseCase {

    override suspend operator fun invoke(deviceUuid: String, taskUid: Long): Outcome<Unit, TaskError> =
        tasksRepository.requestTaskStateInfoSync(deviceUuid, taskUid)
}
