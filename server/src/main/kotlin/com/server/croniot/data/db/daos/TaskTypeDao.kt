package com.server.croniot.data.db.daos

import croniot.models.Device
import croniot.models.TaskType

interface TaskTypeDao {

    fun getId(deviceId: Long, taskTypeUid: Long) : Long?

    fun get(device: Device, taskTypeUid: Long): TaskType?
    fun getLazy(device: Device, taskTypeUid: Long): TaskType?

   // fun insert(/*device: Device, */task: TaskType, deviceId: Long) // : Long
   fun upsert(taskType: TaskType, deviceId: Long) : Long

    fun getByDeviceIds(deviceIds: List<Long>): Map<Long, List<TaskType>>



    fun exists(taskTypeUid: Long, deviceId: Long): Boolean
}
