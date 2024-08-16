package com.croniot.server.db.daos

import croniot.models.Sensor
import croniot.models.SensorInfoDb

interface SensorDao {

    fun insert(sensor: Sensor) : Long

    fun getColumnsUuidId() : List<SensorInfoDb>

}