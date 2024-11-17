package com.croniot.server.db.daos

import croniot.models.SensorData

interface SensorDataDao {

    //fun insert(uuid: String, id_sensor: String, value : String)
    fun insert(sensorData: SensorData)

}