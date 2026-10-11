package com.server.croniot.data.db.daos

import croniot.models.SensorType

interface SensorTypeDao {

    fun upsert(sensorType: SensorType, deviceId: Long): Long?

    fun getByDeviceIds(deviceIds: List<Long>): Map<Long, List<SensorType>>

    // Resolves the device-facing `uid` (from a CBOR sensor batch or the
    // legacy per-reading message) to the internal PK `sensor_type.id`
    // that `sensor_data.sensortype` actually references.
    fun getId(deviceId: Long, uid: Long): Long?
}
