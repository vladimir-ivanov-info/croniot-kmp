package com.croniot.server.db.daos

import croniot.models.TaskStateInfo

interface TaskStateInfoDao {

    fun insert(taskConfigurationStateInfo: TaskStateInfo) : Long

}