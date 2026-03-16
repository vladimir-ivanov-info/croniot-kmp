package com.croniot.client.domain.usecases

import Outcome
import com.croniot.client.domain.errors.TaskError

interface SendNewTaskUseCase {
    suspend operator fun invoke(
        deviceUuid: String,
        taskTypeUid: Long,
        parametersValues: Map<Long, String>
    ): Outcome<Unit, TaskError>
}
