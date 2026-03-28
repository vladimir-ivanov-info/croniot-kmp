package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.TaskDao
import com.server.croniot.data.db.daos.TaskStateInfoDao
import croniot.models.Task
import croniot.models.TaskStateInfo
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val taskStateInfoDao: TaskStateInfoDao,
) {

    fun get(deviceUuid: String, taskTypeUid: Long, taskUid: Long): Task? {
        return taskDao.get(deviceUuid, taskTypeUid, taskUid)
    }

    fun createTaskState(taskStateInfo: TaskStateInfo, taskId: Long) {
        taskStateInfoDao.insert(taskStateInfo, taskId)
    }

    fun create(taskTypeId: Long, taskTypeUid: Long): Task? {
        return taskDao.create(taskTypeId, taskTypeUid)
    }

    fun create(task: Task) {
        taskDao.insert(task)
    }

    fun createState(task: Task, taskStateInfo: TaskStateInfo) {
        // TODO taskStateInfoDao.insert(task, taskStateInfo)
    }

    fun getAll(deviceUuid: String): List<Task> {
        return taskDao.getAll(deviceUuid)
    }

    fun getAllStateInfoHistory(deviceUuid: String): List<TaskStateInfoHistoryEntryDto> {
        return taskDao.getAllStateInfoHistory(deviceUuid)
    }
}
