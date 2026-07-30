package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.ParameterTaskDao
import croniot.models.ParameterTask

class FakeParameterTaskDao : ParameterTaskDao {

    private val byTaskTypeId = mutableMapOf<Long, MutableList<ParameterTask>>()

    fun seed(taskTypeId: Long, parameterTask: ParameterTask) {
        byTaskTypeId.getOrPut(taskTypeId) { mutableListOf() }.add(parameterTask)
    }

    override fun getByUid(parameterTaskUid: Long, taskTypeId: Long): ParameterTask? =
        byTaskTypeId[taskTypeId]?.firstOrNull { it.uid == parameterTaskUid }
}
