package com.server.croniot.data.db.daos

import croniot.models.TaskStateInfo

interface TaskStateInfoDao {

    // fun insert(task: Task, taskStateInfo: TaskStateInfo): Long
    fun insert(taskStateInfo: TaskStateInfo, taskId: Long): Long
}
