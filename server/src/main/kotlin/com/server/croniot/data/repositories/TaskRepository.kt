package com.server.croniot.data.repositories

import com.croniot.server.db.daos.TaskStateInfoDao
import com.croniot.server.db.daos.TaskTypeDao
import croniot.models.Device
import croniot.models.Task
import croniot.models.TaskStateInfo
import croniot.models.TaskType
import db.daos.TaskDao
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val taskStateInfoDao: TaskStateInfoDao
) {

    fun getLazy(deviceUuid: String, taskTypeUid: Long, taskUid: Long) : Task? {
        return taskDao.getLazy(deviceUuid, taskTypeUid, taskUid)
    }

    fun createTaskState(taskStateInfo: TaskStateInfo) {
        taskStateInfoDao.insert(taskStateInfo)
    }

    fun create(device: Device, taskType: TaskType) : Task {
        return taskDao.create(device, taskType)
    }

    fun create(task: Task){
        taskDao.insert(task)
    }

    fun createState(taskStateInfo: TaskStateInfo){
        taskStateInfoDao.insert(taskStateInfo)
    }

    fun getAll(deviceUuid: String) : List<Task> {
        return taskDao.getAll(deviceUuid)
    }

}