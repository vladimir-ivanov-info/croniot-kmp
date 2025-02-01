package com.server.croniot.data.db.daos

import croniot.models.TaskStateInfo

interface TaskStateInfoDao {

    fun insert(taskStateInfo: TaskStateInfo) : Long

}