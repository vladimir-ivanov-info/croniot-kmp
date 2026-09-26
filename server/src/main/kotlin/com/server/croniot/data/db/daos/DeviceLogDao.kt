package com.server.croniot.data.db.daos

import com.server.croniot.mqtt.DeviceLogRecord
import croniot.models.dto.DeviceLogEntryDto
import java.time.OffsetDateTime

interface DeviceLogDao {
    // Returns how many rows were actually new (ON CONFLICT DO NOTHING on
    // the (device, boot_id, seq) unique constraint absorbs resends -
    // plan §5 point 6's "entrega al menos una vez + idempotencia").
    fun insertBatch(deviceId: Long, bootId: Long, records: List<DeviceLogRecord>): Int

    fun getRecent(
        deviceId: Long,
        limit: Int,
        before: OffsetDateTime?,
        beforeId: Long?,
        minLevel: Int?,
    ): List<DeviceLogEntryDto>
}
