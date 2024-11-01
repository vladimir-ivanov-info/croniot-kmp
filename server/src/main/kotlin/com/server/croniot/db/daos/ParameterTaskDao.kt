package com.croniot.server.db.daos

import croniot.models.ParameterTask
import croniot.models.TaskType

interface ParameterTaskDao {

    fun getByUid(parameterTaskUid: Long, taskType: TaskType) : ParameterTask?
  //  fun getLazy(parameterTaskUid: Long, taskType: TaskType) : ParameterTask?

}