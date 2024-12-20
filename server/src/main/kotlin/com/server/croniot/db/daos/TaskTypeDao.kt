package com.croniot.server.db.daos

import croniot.models.Device
import croniot.models.TaskType

interface TaskTypeDao {

    fun insert(task: TaskType) : Long

    fun get(device: Device, taskTypeUid: Long) : TaskType?
    fun getLazy(device: Device, taskUid: Long) : TaskType?

    fun exists(device: Device, taskUid: Long) : Boolean

}