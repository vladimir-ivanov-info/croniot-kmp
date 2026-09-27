package com.server.croniot.data.db.daos

import com.server.croniot.mqtt.DeviceLogRecord
import croniot.models.dto.DeviceLogEntryDto
import java.time.OffsetDateTime

// Structurally identical to DeviceLogDao (croniot-iot's wire record shape
// is the same for Logs and Events, distinguished only by which MQTT topic
// it arrived on) - kept as a separate interface/table rather than one
// generic DAO because device_log and device_event are separate tables
// with separate retention windows (see schema.sql), not a shared one
// with a `stream` discriminator column.
interface DeviceEventDao {
    fun insertBatch(deviceId: Long, bootId: Long, records: List<DeviceLogRecord>): Int

    fun getRecent(
        deviceId: Long,
        limit: Int,
        before: OffsetDateTime?,
        beforeId: Long?,
        minLevel: Int?,
    ): List<DeviceLogEntryDto>
}
