package com.croniot.server.db.daos

import croniot.models.SensorType

interface SensorTypeDao {

    fun insert(sensorType: SensorType) : Long

    fun getLazy(deviceUuid: String, sensorTypeUid: Long) : SensorType?

}