package com.server.croniot.data.db.daos

import com.server.croniot.services.FirmwareImageInfo
import java.time.OffsetDateTime

data class FirmwareRelease(
    val id: Long,
    val project: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
    val createdAt: OffsetDateTime,
)

data class DeviceOtaRequest(
    val deviceId: Long,
    val releaseId: Long,
    val requestId: String,
    val requestedAt: OffsetDateTime,
)

interface FirmwareDao {
    // null when a release with the same project and version already exists.
    fun insertRelease(info: FirmwareImageInfo, image: ByteArray): Long?
    fun listReleases(): List<FirmwareRelease>
    fun getRelease(id: Long): FirmwareRelease?
    fun getImage(id: Long): ByteArray?

    fun upsertRequest(deviceId: Long, releaseId: Long, requestId: String)
    fun getRequest(deviceId: Long): DeviceOtaRequest?
    fun deleteRequest(deviceId: Long): Boolean
}
