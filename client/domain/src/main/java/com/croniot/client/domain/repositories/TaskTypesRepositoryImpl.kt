package com.croniot.client.domain.repositories

import com.croniot.client.core.models.TaskType

class TaskTypesRepositoryImpl : TaskTypesRepository {

    val cache = mutableMapOf<String, TaskType>()

    override fun add(deviceUuid: String, taskType: TaskType) {
        val key = deviceUuid + "_" + taskType.uid.toString()

        if(!cache.containsKey(key)){
            cache[key] = taskType
        }
    }

    override fun get(deviceUuid: String, taskTypeUid: Long) : TaskType? {
        val key = deviceUuid + "_" + taskTypeUid.toString()
        return cache[key]
    }
}