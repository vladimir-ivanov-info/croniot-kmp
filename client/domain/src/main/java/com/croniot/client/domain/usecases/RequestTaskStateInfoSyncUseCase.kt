package com.croniot.client.domain.usecases

import com.croniot.client.domain.errors.TaskError
import Outcome

interface RequestTaskStateInfoSyncUseCase {
    suspend operator fun invoke(deviceUuid: String, taskUid: Long): Outcome<Unit, TaskError>
}
