package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.SensorTypeDao
import croniot.models.Device
import croniot.models.SensorType
import javax.inject.Inject

class SensorTypeRepository @Inject constructor(
    private val sensorTypeDao: SensorTypeDao,
) {

    fun insert(device: Device, sensorType: SensorType) {
        sensorTypeDao.insert(device, sensorType)
    }
}
