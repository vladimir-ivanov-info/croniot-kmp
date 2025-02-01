package com.server.croniot.data.db.daos

import croniot.models.Device
import croniot.models.Task
import croniot.models.TaskType

interface TaskDao {

    fun create(device: Device, taskType: TaskType): Task

    fun insert(task: Task): Long

    fun get(deviceUuid: String, taskTypeUid: Long, taskUid: Long): Task?
    fun getLazy(deviceUuid: String, taskTypeUid: Long, taskUid: Long): Task?
    fun getTaskWithIdOnly(taskUid: Long): Task?

    fun getAll(deviceUuid: String): List<Task>
}
