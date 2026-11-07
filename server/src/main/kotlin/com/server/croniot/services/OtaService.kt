package com.server.croniot.services

import com.server.croniot.application.DomainException
import com.server.croniot.data.db.daos.DeviceFirmware
import com.server.croniot.data.db.daos.FirmwareRelease
import com.server.croniot.data.db.daos.OtaUpdate
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.FirmwareRepository
import com.server.croniot.ota.OtaEvents
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

    data class Upload(val release: FirmwareRelease, val created: Boolean)

    // Uploading the very same file again returns the release it already
    // is, so dropping a .bin a second time just works. The same version
    // with different bytes (a rebuild from uncommitted changes, say) is a
    // conflict: a device can't tell the two apart.
    fun upload(image: ByteArray): Upload {
        val info = FirmwareImage.inspect(image)
            ?: throw DomainException(DomainError.Validation("image", "Not an ESP-IDF app image"))
        val id = firmwareRepository.insertRelease(info, image)
        if (id != null) return Upload(firmwareRepository.getRelease(id)!!, created = true)

        val existing = firmwareRepository.findRelease(info.project, info.version)
        if (existing != null && existing.sha256 == info.sha256) return Upload(existing, created = false)
        throw DomainException(
            DomainError.Conflict(
                "${info.project} ${info.version} is already uploaded with different content. " +
                    "Commit the changes and build again: the version comes from git describe."
            )
        )
    }

    fun listReleases(): List<FirmwareRelease> = firmwareRepository.listReleases()

    // A new requestId every time, also for a release the device was asked
    // for before: that is how a device tells a deliberate retry apart from
    // the same retained request arriving again after a reconnect.
    fun requestUpdate(deviceUuid: String, releaseId: Long): OtaManifestDto {
        val deviceId = deviceId(deviceUuid)
        val release = firmwareRepository.getRelease(releaseId)
            ?: throw DomainException(DomainError.NotFound("Firmware release $releaseId"))
        val installed = firmwareRepository.getFirmware(deviceId)
        // Only checked when the device has reported what it runs: firmware
        // older than firmware_info is still updatable.
        if (installed != null && installed.project != release.project) {
            throw DomainException(
                DomainError.Conflict(
                    "This firmware is for ${release.project}, but the device runs ${installed.project}"
                )
            )
        }
        if (installed != null && installed.version == release.version) {
            // The device would ignore it without a word.
            throw DomainException(DomainError.Conflict("The device already runs ${release.version}"))
        }
        firmwareRepository.latestOpenUpdate(deviceId)?.let { open ->
            if (open.status in INSTALLING) {
                throw DomainException(
                    DomainError.Conflict("Wait for the update to ${open.version} to finish (${open.status})")
                )
            }
        }
        val requestId = UUID.randomUUID().toString()
        firmwareRepository.finishOpenUpdates(deviceId, OtaEvents.CANCELLED, "replaced by a newer request")
        firmwareRepository.upsertRequest(deviceId, release.id, requestId)
        firmwareRepository.insertUpdate(deviceId, release.id, requestId, release.version, installed?.version)
        return manifest(release, requestId)
    }

    fun overview(deviceUuid: String, historyLimit: Int = 20): Overview {
        val deviceId = deviceId(deviceUuid)
        val installed = firmwareRepository.getFirmware(deviceId)
        val release = installed?.imageHash?.let { firmwareRepository.findReleaseByImageHash(it) }
            ?.takeIf { it.project == installed.project && it.version == installed.version }
        val history = firmwareRepository.history(deviceId, historyLimit)
        return Overview(
            installed = installed,
            installedRelease = release,
            current = history.firstOrNull { !OtaEvents.isFinished(it.status) },
            history = history,
        )
    }

    data class Overview(
        val installed: DeviceFirmware?,
        val installedRelease: FirmwareRelease?,
        val current: OtaUpdate?,
        val history: List<OtaUpdate>,
    )

    fun currentRequest(deviceUuid: String): OtaManifestDto? {
        val request = firmwareRepository.getRequest(deviceId(deviceUuid)) ?: return null
        val release = firmwareRepository.getRelease(request.releaseId) ?: return null
        return manifest(release, request.requestId)
    }

    fun cancel(deviceUuid: String): Boolean {
        val deviceId = deviceId(deviceUuid)
        firmwareRepository.latestOpenUpdate(deviceId)?.let { open ->
            if (open.status in INSTALLING) {
                throw DomainException(
                    DomainError.Conflict("Too late to cancel: the device is already installing ${open.version}")
                )
            }
        }
        firmwareRepository.finishOpenUpdates(deviceId, OtaEvents.CANCELLED, "cancelled from admin-ui")
        return firmwareRepository.deleteRequest(deviceId)
    }

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
        // Past the download: the device finishes on its own, and the
        // result still has to land on this update's row.
        private val INSTALLING = setOf(
            OtaEvents.INSTALLED,
            OtaEvents.VALIDATING,
            OtaEvents.ROLLING_BACK,
            OtaEvents.PROBING,
        )

        fun firmwarePath(releaseId: Long) = "/api/iot/firmware/$releaseId"
    }
}
