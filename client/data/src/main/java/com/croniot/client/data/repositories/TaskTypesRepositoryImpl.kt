package com.croniot.client.data.repositories

import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.repositories.TaskTypesRepository
import java.util.concurrent.ConcurrentHashMap

class TaskTypesRepositoryImpl : TaskTypesRepository {

    private val cache = ConcurrentHashMap<String, TaskType>()

    override fun add(deviceUuid: String, taskType: TaskType) {
        val key = deviceUuid + "_" + taskType.uid.toString()
        cache.putIfAbsent(key, taskType)
    }

    override fun get(deviceUuid: String, taskTypeUid: Long): TaskType? {
        val key = deviceUuid + "_" + taskTypeUid.toString()
        return cache[key]
    }

    override fun getAll(deviceUuid: String): List<TaskType> {
        val prefix = deviceUuid + "_"
        return cache.entries
            .filter { it.key.startsWith(prefix) }
            .map { it.value }
    }
}