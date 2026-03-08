package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.ParameterTaskDao
import com.server.croniot.data.db.daos.TaskTypeDao
import croniot.models.Device
import croniot.models.ParameterTask
import croniot.models.TaskType
import javax.inject.Inject

class TaskTypeRepository @Inject constructor(
    private val taskTypeDao: TaskTypeDao,
    private val parameterTaskDao: ParameterTaskDao,
) {

    fun getId(deviceId: Long, taskTypeUid: Long) : Long? {
        return taskTypeDao.getId(deviceId, taskTypeUid)
    }

    fun get(device: Device, taskTypeUid: Long): TaskType? {
        return taskTypeDao.get(device, taskTypeUid)
    }

    fun exists(taskTypeUid: Long, deviceId: Long): Boolean {
        return taskTypeDao.exists(taskTypeUid = taskTypeUid, deviceId = deviceId)
    }

    fun insert(/*device: Device,*/ taskType: TaskType, deviceId: Long) {
        taskTypeDao.upsert(/*device, */taskType, deviceId)
    }

    fun getLazy(device: Device, taskTypeUid: Long): TaskType? {
        return taskTypeDao.getLazy(device, taskTypeUid)
    }

    fun getParameterTaskByUid(parameterUid: Long, taskTypeId: Long/*, taskType: TaskType*/): ParameterTask? {
        return parameterTaskDao.getByUid(parameterUid, taskTypeId)
    }
}
