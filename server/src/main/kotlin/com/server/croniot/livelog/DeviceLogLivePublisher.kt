package com.server.croniot.livelog

import com.server.croniot.data.db.daos.InsertedDeviceLogRow
import javax.inject.Inject

// Turns device_log / device_event rows that were just inserted into live
// entries. Called by DeviceLogService *after* the INSERT, with only the
// rows the database really created - so the viewer shows exactly what
// history holds, and a resent batch shows nothing.
class DeviceLogLivePublisher @Inject constructor(
    private val bus: LiveLogBus,
    private val deviceContextLookup: DeviceContextLookup,
) {
    fun publish(deviceId: Long, bootId: Long, kind: LogKind, rows: List<InsertedDeviceLogRow>) {
        // Nobody watching: skip the device/owner lookup entirely, so the
        // live viewer costs nothing while it is closed.
        if (rows.isEmpty() || bus.subscriberCount == 0) return

        val context = deviceContextLookup.byDeviceId(deviceId)
        val prefix = if (kind == LogKind.EVENT) "evt" else "log"

        bus.publishAll(
            rows.map { row ->
                LogEntryDto(
                    id = "$prefix-${row.id}",
                    platform = LogPlatform.IOT,
                    kind = kind,
                    level = row.record.level,
                    tag = row.record.tag,
                    message = row.record.message,
                    receivedAtMs = row.receivedAt.toInstant().toEpochMilli(),
                    device = context?.device,
                    owner = context?.owner,
                    bootId = bootId,
                    seq = row.record.seq,
                    uptimeMs = row.record.uptimeMs,
                )
            },
        )
    }
}
