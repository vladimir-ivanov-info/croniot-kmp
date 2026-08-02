package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.SensorTypeDao
import croniot.models.SensorType

class FakeSensorTypeDao : SensorTypeDao {

    private val byDeviceId = mutableMapOf<Long, MutableList<SensorType>>()
    private var nextId = 1L

    fun seed(deviceId: Long, sensorType: SensorType) {
        byDeviceId.getOrPut(deviceId) { mutableListOf() }.add(sensorType)
    }

    override fun upsert(sensorType: SensorType, deviceId: Long): Long? {
        byDeviceId.getOrPut(deviceId) { mutableListOf() }.add(sensorType)
        return nextId++
    }

    override fun getByDeviceIds(deviceIds: List<Long>): Map<Long, List<SensorType>> =
        deviceIds.associateWith { byDeviceId[it] ?: emptyList() }
}
