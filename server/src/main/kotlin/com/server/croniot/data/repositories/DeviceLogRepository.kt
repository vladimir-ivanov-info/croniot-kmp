package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.DeviceLogDao
import com.server.croniot.mqtt.DeviceLogRecord
import croniot.models.dto.DeviceLogEntryDto
import java.time.OffsetDateTime
import javax.inject.Inject

class DeviceLogRepository @Inject constructor(
    private val deviceLogDao: DeviceLogDao,
) {

    fun insertBatch(deviceId: Long, bootId: Long, records: List<DeviceLogRecord>): Int {
        return deviceLogDao.insertBatch(deviceId, bootId, records)
    }

    fun getRecent(
        deviceId: Long,
        limit: Int,
        before: OffsetDateTime?,
        beforeId: Long?,
        minLevel: Int?,
    ): List<DeviceLogEntryDto> {
        return deviceLogDao.getRecent(deviceId, limit, before, beforeId, minLevel)
    }
}
