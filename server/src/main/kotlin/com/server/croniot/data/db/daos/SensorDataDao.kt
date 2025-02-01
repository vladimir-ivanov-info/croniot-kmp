package com.server.croniot.data.db.daos

import croniot.models.SensorData

interface SensorDataDao {

    fun insert(sensorData: SensorData)
}
