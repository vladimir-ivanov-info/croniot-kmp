package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.SensorTypeDao
import croniot.models.SensorType

class FakeSensorTypeDao : SensorTypeDao {

    private val byDeviceId = mutableMapOf<Long, MutableList<SensorType>>()
    private val idByDeviceAndUid = mutableMapOf<Pair<Long, Long>, Long>()
    private var nextId = 1L

    fun seed(deviceId: Long, sensorType: SensorType) {
        byDeviceId.getOrPut(deviceId) { mutableListOf() }.add(sensorType)
        idByDeviceAndUid[deviceId to sensorType.uid] = nextId++
    }

    override fun upsert(sensorType: SensorType, deviceId: Long): Long? {
        byDeviceId.getOrPut(deviceId) { mutableListOf() }.add(sensorType)
        val id = nextId++
        idByDeviceAndUid[deviceId to sensorType.uid] = id
        return id
    }

    override fun getByDeviceIds(deviceIds: List<Long>): Map<Long, List<SensorType>> =
        deviceIds.associateWith { byDeviceId[it] ?: emptyList() }

    override fun getId(deviceId: Long, uid: Long): Long? = idByDeviceAndUid[deviceId to uid]
}
