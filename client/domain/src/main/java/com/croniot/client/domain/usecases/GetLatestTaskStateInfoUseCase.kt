package com.croniot.client.domain.usecases

import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.domain.repositories.TasksRepository

class GetLatestTaskStateInfoUseCase(
    private val tasksRepository: TasksRepository,
) {
    operator fun invoke(deviceUuid: String, taskTypeUid: Long): TaskStateInfo? =
        tasksRepository.getLatestTaskStateInfo(deviceUuid, taskTypeUid)
}
