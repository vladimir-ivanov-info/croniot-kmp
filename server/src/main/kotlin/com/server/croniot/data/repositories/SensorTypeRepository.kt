package com.server.croniot.data.repositories

import com.croniot.server.db.daos.AccountDao
import com.croniot.server.db.daos.SensorTypeDao
import croniot.models.SensorType
import javax.inject.Inject

class SensorTypeRepository @Inject constructor(
    private val sensorTypeDao: SensorTypeDao
) {

    fun create(sensorType: SensorType){
        sensorTypeDao.insert(sensorType)
    }

}