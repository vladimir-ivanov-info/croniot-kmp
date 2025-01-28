package com.server.croniot.data.repositories

import com.croniot.server.db.daos.ParameterTaskDao
import com.croniot.server.db.daos.TaskTypeDao
import croniot.models.Device
import croniot.models.ParameterTask
import croniot.models.TaskType
import javax.inject.Inject

class TaskTypeRepository @Inject constructor(
    private val taskTypeDao: TaskTypeDao,
    private val parameterTaskDao: ParameterTaskDao
){

    fun get(device: Device, taskTypeUid: Long) : TaskType? {
        return taskTypeDao.get(device, taskTypeUid)
    }

    fun exists(device: Device, taskTypeUid: Long) : Boolean {
        return taskTypeDao.exists(device, taskTypeUid)
    }

    fun create(taskType: TaskType){
        taskTypeDao.insert(taskType)
    }

    fun getLazy(device: Device, taskTypeUid: Long) : TaskType? {
        return taskTypeDao.getLazy(device, taskTypeUid)
    }

    fun getParameterTaskByUid(parameterUid: Long, taskType: TaskType) : ParameterTask? {
        return parameterTaskDao.getByUid(parameterUid, taskType)
    }

}