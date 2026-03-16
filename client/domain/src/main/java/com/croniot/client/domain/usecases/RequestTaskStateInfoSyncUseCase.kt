package com.croniot.client.domain.usecases

import Outcome
import com.croniot.client.domain.errors.TaskError

interface RequestTaskStateInfoSyncUseCase {
    suspend operator fun invoke(deviceUuid: String, taskUid: Long): Outcome<Unit, TaskError>
}
