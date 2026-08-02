package com.croniot.testing.fakes

import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.repositories.TaskTypesRepository

class FakeTaskTypesRepository : TaskTypesRepository {

    private val store: MutableMap<Pair<String, Long>, TaskType> = mutableMapOf()

    var addInvocations: MutableList<Pair<String, TaskType>> = mutableListOf()
        private set

    override fun add(deviceUuid: String, taskType: TaskType) {
        addInvocations += deviceUuid to taskType
        store.putIfAbsent(deviceUuid to taskType.uid, taskType)
    }

    override fun get(deviceUuid: String, taskTypeUid: Long): TaskType? = store[deviceUuid to taskTypeUid]

    override fun getAll(deviceUuid: String): List<TaskType> =
        store.filterKeys { it.first == deviceUuid }.values.toList()
}
