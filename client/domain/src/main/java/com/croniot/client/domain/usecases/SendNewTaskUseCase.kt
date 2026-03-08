package com.croniot.client.domain.usecases

import com.croniot.client.domain.errors.TaskError
import Outcome

interface SendNewTaskUseCase {
    suspend operator fun invoke(deviceUuid: String, taskTypeUid: Long, parametersValues: Map<Long, String>): Outcome<Unit, TaskError>
}
