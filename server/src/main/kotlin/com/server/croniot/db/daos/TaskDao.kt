package db.daos

import croniot.models.Task

interface TaskDao {

    fun insert(task: Task) : Long

    fun get(deviceUuid: String, taskTypeUid: Long, taskUid: Long) : Task?

    fun getAll(deviceUuid: String) : List<Task>


}