package com.croniot.client.domain.usecases

import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.domain.repositories.TasksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class ObserveTaskStateInfoUseCase(
    private val tasksRepository: TasksRepository,
) {
    operator fun invoke(deviceUuid: String, taskTypeUid: Long? = null): Flow<TaskStateInfo> =
        tasksRepository.observeTaskStateInfoUpdates(deviceUuid)
            .let { flow ->
                if (taskTypeUid != null) flow.filter { it.key.taskTypeUid == taskTypeUid } else flow
            }
            .map { it.info }
}
