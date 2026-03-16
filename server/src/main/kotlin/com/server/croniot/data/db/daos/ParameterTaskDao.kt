package com.server.croniot.data.db.daos

import croniot.models.ParameterTask

interface ParameterTaskDao {

    // fun getByUid(parameterTaskUid: Long, taskType: TaskType): ParameterTask?
    fun getByUid(parameterTaskUid: Long, taskTypeId: Long): ParameterTask?
}
