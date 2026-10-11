package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.SensorDataDao
import java.time.OffsetDateTime
import javax.inject.Inject

class SensorDataRepository @Inject constructor(
    private val sensorDataDao: SensorDataDao,
) {
    fun insertBatch(
        deviceId: Long,
        sensorTypeId: Long,
        bootId: Long,
        seq: Long,
        values: List<Double>,
        receivedAt: OffsetDateTime,
    ): Int {
        return sensorDataDao.insertBatch(deviceId, sensorTypeId, bootId, seq, values, receivedAt)
    }
}
