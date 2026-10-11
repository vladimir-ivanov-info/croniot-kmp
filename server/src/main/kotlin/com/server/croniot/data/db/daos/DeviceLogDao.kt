package com.server.croniot.data.db.daos

import com.server.croniot.mqtt.DeviceLogRecord
import croniot.models.dto.DeviceLogEntryDto
import java.time.OffsetDateTime

// A row insertBatch() actually created, with the id and received_at the
// database assigned - what the live viewer needs to show it exactly as
// history will.
data class InsertedDeviceLogRow(
    val id: Long,
    val record: DeviceLogRecord,
    val receivedAt: OffsetDateTime,
)

interface DeviceLogDao {
    // Returns only the rows that were actually new (ON CONFLICT DO
    // NOTHING on the (device, boot_id, seq) unique constraint absorbs
    // resends - plan §5 point 6's "entrega al menos una vez +
    // idempotencia"), so a resent batch publishes nothing live.
    fun insertBatch(deviceId: Long, bootId: Long, records: List<DeviceLogRecord>): List<InsertedDeviceLogRow>

    fun getRecent(
        deviceId: Long,
        limit: Int,
        before: OffsetDateTime?,
        beforeId: Long?,
        minLevel: Int?,
    ): List<DeviceLogEntryDto>
}
