package com.server.croniot.data.db.daos

import java.time.OffsetDateTime

// One update request and how it went (table device_ota_update).
data class OtaUpdate(
    val id: Long,
    val deviceId: Long,
    val releaseId: Long?,
    val requestId: String,
    val version: String,
    val fromVersion: String?,
    val status: String,
    val progress: Int,
    val detail: String?,
    val requestedAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val finishedAt: OffsetDateTime?,
)

// What a device said it runs, from its firmware_info event.
data class DeviceFirmware(
    val project: String,
    val version: String,
    val builtAt: String?,
    val idfVersion: String?,
    val sizeBytes: Long?,
    val imageHash: String?,
    val slot: String?,
    val reportedAt: OffsetDateTime,
)

interface OtaHistoryDao {
    fun insertUpdate(
        deviceId: Long,
        releaseId: Long,
        requestId: String,
        version: String,
        fromVersion: String?,
    )

    // Closes every unfinished update of the device as `status`.
    fun finishOpen(deviceId: Long, status: String, detail: String)

    fun findByRequestId(requestId: String): OtaUpdate?

    // The device's newest update that hasn't finished.
    fun latestOpen(deviceId: Long): OtaUpdate?

    fun updateState(
        id: Long,
        status: String,
        progress: Int,
        detail: String?,
        finished: Boolean,
        fromVersion: String?,
    )

    fun listForDevice(deviceId: Long, limit: Int): List<OtaUpdate>

    fun upsertFirmware(deviceId: Long, firmware: DeviceFirmware)
    fun getFirmware(deviceId: Long): DeviceFirmware?
}
