package com.croniot.server.db.daos

import croniot.models.Device
import croniot.models.TaskType

interface TaskTypeDao {

    fun insert(task: TaskType) : Long

    fun get(device: Device, taskUid: Long) : TaskType?

}