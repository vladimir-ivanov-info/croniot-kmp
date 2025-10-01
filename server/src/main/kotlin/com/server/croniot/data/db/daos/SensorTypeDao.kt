package com.server.croniot.data.db.daos

import croniot.models.Device
import croniot.models.SensorType

interface SensorTypeDao {

    fun insert(device: Device, sensorType: SensorType) // : Long

    fun getLazy(deviceUuid: String, sensorTypeUid: Long): SensorType?
}
