package com.server.croniot.data.db.daos

import java.time.OffsetDateTime

interface SensorDataDao {
    // Inserts one row per value in `values`, `sample_index` = position
    // in that list. Returns how many rows were actually new (ON
    // CONFLICT DO NOTHING on the partial unique index absorbs a
    // resent/retried flush - same idempotency contract as
    // DeviceLogDao/DeviceEventDao).
    fun insertBatch(
        deviceId: Long,
        sensorTypeId: Long,
        bootId: Long,
        seq: Long,
        values: List<Double>,
        receivedAt: OffsetDateTime,
    ): Int
}
