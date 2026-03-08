package com.server.croniot.data.db.daos

import croniot.models.ParameterTask
import croniot.models.TaskType

interface ParameterTaskDao {

    //fun getByUid(parameterTaskUid: Long, taskType: TaskType): ParameterTask?
    fun getByUid(parameterTaskUid: Long, taskTypeId: Long): ParameterTask?
}
