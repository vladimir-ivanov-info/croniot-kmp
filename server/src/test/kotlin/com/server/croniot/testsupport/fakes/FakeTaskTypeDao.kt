package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.TaskTypeDao
import croniot.models.Device
import croniot.models.TaskType

class FakeTaskTypeDao : TaskTypeDao {

    private val byDeviceId = mutableMapOf<Long, MutableList<TaskType>>()
    private val byDeviceUuid = mutableMapOf<String, MutableList<TaskType>>()
    private var nextId = 1L

    fun seed(deviceId: Long, taskType: TaskType) {
        byDeviceId.getOrPut(deviceId) { mutableListOf() }.add(taskType)
    }

    /** Seeds a task type reachable via [get]/[getLazy], which are keyed by the [Device] itself (its uuid), not by device id. */
    fun seedForDevice(device: Device, taskType: TaskType) {
        byDeviceUuid.getOrPut(device.uuid) { mutableListOf() }.add(taskType)
    }

    override fun getId(deviceId: Long, taskTypeUid: Long): Long? =
        byDeviceId[deviceId]?.firstOrNull { it.uid == taskTypeUid }?.let { nextId }

    override fun get(device: Device, taskTypeUid: Long): TaskType? =
        byDeviceUuid[device.uuid]?.firstOrNull { it.uid == taskTypeUid }

    override fun getLazy(device: Device, taskTypeUid: Long): TaskType? = get(device, taskTypeUid)

    override fun upsert(taskType: TaskType, deviceId: Long): Long {
        byDeviceId.getOrPut(deviceId) { mutableListOf() }.add(taskType)
        return nextId++
    }

    override fun getByDeviceIds(deviceIds: List<Long>): Map<Long, List<TaskType>> =
        deviceIds.associateWith { byDeviceId[it] ?: emptyList() }

    override fun exists(taskTypeUid: Long, deviceId: Long): Boolean =
        byDeviceId[deviceId]?.any { it.uid == taskTypeUid } ?: false
}
