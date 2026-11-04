package com.server.croniot.services

import com.server.croniot.application.DomainException
import com.server.croniot.data.db.daos.FirmwareRelease
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.FirmwareRepository
import com.server.croniot.ota.OtaManifestDto
import croniot.models.errors.DomainError
import java.util.UUID
import javax.inject.Inject

// Firmware releases and the per-device requests to install them
// (documentation/plans/ota.md §7). The device side, including deciding
// whether a requested release is installed and kept, is croniot-iot's
// ota/OtaController.cpp; this side only stores, offers and serves.
class OtaService @Inject constructor(
    private val firmwareRepository: FirmwareRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    fun upload(image: ByteArray): FirmwareRelease {
        val info = FirmwareImage.inspect(image)
            ?: throw DomainException(DomainError.Validation("image", "Not an ESP-IDF app image"))
        val id = firmwareRepository.insertRelease(info, image)
            ?: throw DomainException(
                DomainError.Conflict("${info.project} ${info.version} is already uploaded")
            )
        return firmwareRepository.getRelease(id)!!
    }

    fun listReleases(): List<FirmwareRelease> = firmwareRepository.listReleases()

    // A new requestId every time, also for a release the device was asked
    // for before: that is how a device tells a deliberate retry apart from
    // the same retained request arriving again after a reconnect.
    fun requestUpdate(deviceUuid: String, releaseId: Long): OtaManifestDto {
        val deviceId = deviceId(deviceUuid)
        val release = firmwareRepository.getRelease(releaseId)
            ?: throw DomainException(DomainError.NotFound("Firmware release $releaseId"))
        val requestId = UUID.randomUUID().toString()
        firmwareRepository.upsertRequest(deviceId, release.id, requestId)
        return manifest(release, requestId)
    }

    fun currentRequest(deviceUuid: String): OtaManifestDto? {
        val request = firmwareRepository.getRequest(deviceId(deviceUuid)) ?: return null
        val release = firmwareRepository.getRelease(request.releaseId) ?: return null
        return manifest(release, request.requestId)
    }

    fun cancel(deviceUuid: String): Boolean = firmwareRepository.deleteRequest(deviceId(deviceUuid))

    // A device may only download the release it has been asked to install.
    fun imageForDevice(deviceUuid: String?, deviceToken: String?, releaseId: Long): ByteArray {
        if (deviceUuid.isNullOrBlank() || deviceToken.isNullOrBlank() ||
            !deviceTokenRepository.isTokenCorrect(deviceUuid, deviceToken)
        ) {
            throw DomainException(DomainError.Unauthorized("Invalid device credentials"))
        }
        val request = firmwareRepository.getRequest(deviceId(deviceUuid))
        if (request?.releaseId != releaseId) {
            throw DomainException(DomainError.NotFound("Firmware release $releaseId"))
        }
        return firmwareRepository.getImage(releaseId)
            ?: throw DomainException(DomainError.NotFound("Firmware release $releaseId"))
    }

    private fun deviceId(deviceUuid: String): Long =
        deviceRepository.getId(deviceUuid) ?: throw DomainException(DomainError.NotFound("Device $deviceUuid"))

    private fun manifest(release: FirmwareRelease, requestId: String) = OtaManifestDto(
        requestId = requestId,
        version = release.version,
        path = firmwarePath(release.id),
        size = release.sizeBytes,
        sha256 = release.sha256,
    )

    companion object {
        fun firmwarePath(releaseId: Long) = "/api/iot/firmware/$releaseId"
    }
}
