package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.SensorTypeDao
import croniot.models.SensorType
import javax.inject.Inject

class SensorTypeRepository @Inject constructor(
    private val sensorTypeDao: SensorTypeDao
) {

    fun create(sensorType: SensorType){
        sensorTypeDao.insert(sensorType)
    }

}