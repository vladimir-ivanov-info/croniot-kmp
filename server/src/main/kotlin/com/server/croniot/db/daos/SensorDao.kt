package com.croniot.server.db.daos

import croniot.models.SensorType
import croniot.models.SensorInfoDb

interface SensorDao {

    fun insert(sensorType: SensorType) : Long

    fun getLazy(deviceUuid: String, sensorTypeUid: Long) : SensorType?

}