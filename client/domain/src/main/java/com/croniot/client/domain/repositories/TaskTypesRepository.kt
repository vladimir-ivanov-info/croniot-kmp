package com.croniot.client.domain.repositories

import com.croniot.client.core.models.TaskType

interface TaskTypesRepository {

    fun add(deviceUuid: String, taskType: TaskType)

    fun get(deviceUuid: String, taskTypeUid: Long): TaskType?
}
