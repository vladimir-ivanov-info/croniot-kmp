package com.server.croniot.data.db.daos

import croniot.models.SensorType

interface SensorTypeDao {

    fun insert(sensorType: SensorType) : Long

    fun getLazy(deviceUuid: String, sensorTypeUid: Long) : SensorType?

}