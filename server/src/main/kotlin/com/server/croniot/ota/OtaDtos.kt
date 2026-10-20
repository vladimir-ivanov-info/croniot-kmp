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
)

@Serializable
data class OtaUpdateRequestDto(
    val releaseId: Long,
)
