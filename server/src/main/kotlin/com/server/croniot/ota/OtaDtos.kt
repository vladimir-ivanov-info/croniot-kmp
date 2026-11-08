package com.server.croniot.ota

import kotlinx.serialization.Serializable

// What a device receives, retained, on /server/<uuid>/ota. Contract with
// croniot-iot's ota/OtaManifest.h (CRONIOT.md §5): a server path, never a
// URL - the device always downloads from the server it is configured with.
@Serializable
data class OtaManifestDto(
    val requestId: String,
    val version: String,
    val path: String,
    val size: Long,
    val sha256: String,
)

@Serializable
data class FirmwareReleaseDto(
    val id: Long,
    val project: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
    val createdAt: String,
    val builtAt: String? = null,
    val idfVersion: String? = null,
)

@Serializable
data class OtaUpdateRequestDto(
    val releaseId: Long,
)

// What the device runs, from its last firmware_info event. `release` is
// the upload it came from, when its image hash matches one.
@Serializable
data class InstalledFirmwareDto(
    val project: String,
    val version: String,
    val builtAt: String?,
    val idfVersion: String?,
    val sizeBytes: Long?,
    val slot: String?,
    val reportedAt: String,
    val release: FirmwareReleaseDto?,
)

@Serializable
data class OtaUpdateDto(
    val requestId: String,
    val releaseId: Long?,
    val version: String,
    val fromVersion: String?,
    // requested, downloading, retrying, installed, validating, rolling_back,
    // probing; or, once finished: confirmed, failed, rolled_back, ignored,
    // cancelled.
    val status: String,
    val finished: Boolean,
    val progress: Int,
    val detail: String?,
    val requestedAt: String,
    val updatedAt: String,
    val finishedAt: String?,
)

// GET /api/admin/devices/{uuid}/firmware: everything the device page shows.
@Serializable
data class DeviceFirmwareOverviewDto(
    val installed: InstalledFirmwareDto?,
    // The update in progress, if any; also the first entry of `history`.
    val current: OtaUpdateDto?,
    val history: List<OtaUpdateDto>,
)
