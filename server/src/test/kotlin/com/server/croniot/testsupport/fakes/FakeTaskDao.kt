package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.TaskDao
import croniot.models.Task
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import java.time.OffsetDateTime

/**
 * In-memory fake of [TaskDao]. Tasks are stored per device uuid (as the real table associates
 * them via the device -> task type -> task chain), even though the [Task] domain model itself
 * doesn't carry a device uuid.
 */
class FakeTaskDao : TaskDao {

    private val byDeviceUuid = mutableMapOf<String, MutableList<Task>>()
    private val historyByDeviceUuid = mutableMapOf<String, MutableList<TaskStateInfoHistoryEntryDto>>()
    private var nextUid = 1L

    /** Tasks passed to [insert], in order, for tests to assert on directly. */
    val insertedTasks = mutableListOf<Task>()

    /** Overrides what [create] returns; defaults to auto-generating a fresh [Task]. */
    var createResult: Task? = null

    fun seed(deviceUuid: String, task: Task) {
        byDeviceUuid.getOrPut(deviceUuid) { mutableListOf() }.add(task)
    }

    fun seedHistory(deviceUuid: String, entry: TaskStateInfoHistoryEntryDto) {
        historyByDeviceUuid.getOrPut(deviceUuid) { mutableListOf() }.add(entry)
    }

    override fun create(taskTypeId: Long, taskTypeUid: Long): Task? =
        createResult ?: Task(uid = nextUid++, parametersValues = emptyMap(), taskTypeUid = taskTypeUid)

    override fun insert(task: Task): Long {
        insertedTasks.add(task)
        return task.uid
    }

    override fun get(deviceUuid: String, taskTypeUid: Long, taskUid: Long): Task? =
        byDeviceUuid[deviceUuid]?.firstOrNull { it.taskTypeUid == taskTypeUid && it.uid == taskUid }

    override fun getAll(deviceUuid: String): List<Task> = byDeviceUuid[deviceUuid] ?: emptyList()

    override fun getAllStateInfoHistory(
        deviceUuid: String,
        limit: Int,
        before: OffsetDateTime?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): List<TaskStateInfoHistoryEntryDto> =
        (historyByDeviceUuid[deviceUuid] ?: emptyList())
            .filter { taskTypeUid == null || it.taskTypeUid == taskTypeUid }
            .take(limit)

    override fun getAllStateInfoHistoryCount(
        deviceUuid: String,
        before: OffsetDateTime?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Int =
        (historyByDeviceUuid[deviceUuid] ?: emptyList())
            .count { taskTypeUid == null || it.taskTypeUid == taskTypeUid }
}
