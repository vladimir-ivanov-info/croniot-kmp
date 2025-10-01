package com.server.croniot.data.db.daos

import croniot.models.Task
import croniot.models.TaskStateInfo

interface TaskStateInfoDao {

    fun insert(task: Task, taskStateInfo: TaskStateInfo): Long
}
