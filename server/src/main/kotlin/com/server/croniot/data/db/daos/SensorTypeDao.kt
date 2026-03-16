package com.server.croniot.data.db.daos

import croniot.models.SensorType

interface SensorTypeDao {

    fun upsert(sensorType: SensorType, deviceId: Long): Long?

    fun getByDeviceIds(deviceIds: List<Long>): Map<Long, List<SensorType>>
}
