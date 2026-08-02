package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.TaskStateInfoDao
import croniot.models.TaskStateInfo

class FakeTaskStateInfoDao : TaskStateInfoDao {

    /** (taskStateInfo, taskId) pairs passed to [insert], in order, for tests to assert on directly. */
    val insertedStateInfos = mutableListOf<Pair<TaskStateInfo, Long>>()
    private var nextId = 1L

    override fun insert(taskStateInfo: TaskStateInfo, taskId: Long): Long {
        insertedStateInfos.add(taskStateInfo to taskId)
        return nextId++
    }
}
