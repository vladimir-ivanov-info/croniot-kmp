package com.croniot.server.db.daos

import croniot.models.SensorData

interface SensorDataDao {

    fun insert(sensorData: SensorData)

}