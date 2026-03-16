package com.server.croniot.data.db.daos

import croniot.models.Task

interface TaskDao {

    fun create(taskTypeId: Long, taskTypeUid: Long): Task?

    fun insert(task: Task): Long

    fun get(deviceUuid: String, taskTypeUid: Long, taskUid: Long): Task?

    fun getAll(deviceUuid: String): List<Task>
}
