package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.DeviceEventDao
import com.server.croniot.mqtt.DeviceLogRecord
import croniot.models.dto.DeviceLogEntryDto
import java.time.OffsetDateTime
import javax.inject.Inject

class DeviceEventRepository @Inject constructor(
    private val deviceEventDao: DeviceEventDao,
) {

    fun insertBatch(deviceId: Long, bootId: Long, records: List<DeviceLogRecord>): Int {
        return deviceEventDao.insertBatch(deviceId, bootId, records)
    }

    fun getRecent(
        deviceId: Long,
        limit: Int,
        before: OffsetDateTime?,
        beforeId: Long?,
        minLevel: Int?,
    ): List<DeviceLogEntryDto> {
        return deviceEventDao.getRecent(deviceId, limit, before, beforeId, minLevel)
    }
}
