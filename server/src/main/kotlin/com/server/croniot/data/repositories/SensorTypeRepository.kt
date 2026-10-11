package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.SensorTypeDao
import croniot.models.SensorType
import javax.inject.Inject

class SensorTypeRepository @Inject constructor(
    private val sensorTypeDao: SensorTypeDao,
) {

    fun upsert(sensorType: SensorType, deviceId: Long) {
        sensorTypeDao.upsert(sensorType, deviceId)
    }

    fun getId(deviceId: Long, uid: Long): Long? {
        return sensorTypeDao.getId(deviceId, uid)
    }
}
