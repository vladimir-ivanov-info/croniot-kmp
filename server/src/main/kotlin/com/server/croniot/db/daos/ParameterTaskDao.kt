package com.croniot.server.db.daos

import croniot.models.ParameterTask
import croniot.models.TaskType

interface ParameterTaskDao {

    fun getByUid(parameterTaskUid: Long, task: TaskType) : ParameterTask?

}