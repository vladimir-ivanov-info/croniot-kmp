package com.server.croniot.data.db.daos

import croniot.models.Device
import croniot.models.TaskType

interface TaskTypeDao {

    fun insert(device: Device, task: TaskType) //: Long

    fun get(device: Device, taskTypeUid: Long): TaskType?
    fun getLazy(device: Device, taskTypeUid: Long): TaskType?

    fun exists(device: Device, taskTypeUid: Long): Boolean
}
