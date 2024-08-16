package com.croniot.server.db.daos

interface SensorDataDao {

    fun insert(uuid: String, id_sensor: String, value : String)

}