package com.server.croniot.services

import com.server.croniot.data.db.daos.DeviceFirmware
import com.server.croniot.data.repositories.FirmwareRepository
import com.server.croniot.mqtt.DeviceLogRecord
import com.server.croniot.ota.OtaEvents
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import javax.inject.Inject

// Keeps device_ota_update and device_firmware in step with the events a
// device sends (firmware_info once per boot, ota_* during an update), so
// admin-ui can show the installed firmware and each update's progress.
// Called with newly inserted events only: a resent batch changes nothing.
class OtaEventTracker @Inject constructor(
    private val firmwareRepository: FirmwareRepository,
) {
    private val logger = KotlinLogging.logger {}

    fun onEvents(deviceId: Long, records: List<DeviceLogRecord>) {
        // In device order, which is the order the states happened in.
        for (record in records.sortedBy { it.seq }) {
            // One bad record must not cost the others (or the batch's ack).
            runCatching { onEvent(deviceId, record) }
                .onFailure { logger.warn(it) { "device $deviceId: could not apply ${record.tag}" } }
        }
    }

    private fun onEvent(deviceId: Long, record: DeviceLogRecord) {
        if (record.tag == OtaEvents.FIRMWARE_INFO) {
            onFirmwareInfo(deviceId, OtaEvents.parseFields(record.message))
            return
        }
        if (!OtaEvents.isOtaEvent(record.tag)) return
        val fields = OtaEvents.parseFields(record.message)

        // Firmware older than this feature sends no requestId: the newest
        // open update is the one it is working on.
        val update = fields["requestId"]?.takeIf { it.isNotBlank() }
            ?.let { firmwareRepository.findUpdate(it) }
            ?.takeIf { it.deviceId == deviceId }
            ?: firmwareRepository.latestOpenUpdate(deviceId)
            ?: return

        val t = OtaEvents.transition(record.tag, fields, update.status, update.progress) ?: return
        firmwareRepository.updateState(
            id = update.id,
            status = t.status,
            progress = t.progress ?: update.progress,
            detail = t.detail,
            finished = t.finished,
            fromVersion = t.fromVersion,
        )
    }

    private fun onFirmwareInfo(deviceId: Long, fields: Map<String, String>) {
        val project = fields["project"]?.takeIf { it.isNotBlank() } ?: return
        val version = fields["version"]?.takeIf { it.isNotBlank() } ?: return
        firmwareRepository.upsertFirmware(
            deviceId,
            DeviceFirmware(
                project = project.take(32),
                version = version.take(32),
                builtAt = fields["built"]?.takeIf { it.isNotBlank() }?.take(20),
                idfVersion = fields["idf"]?.takeIf { it.isNotBlank() }?.take(32),
                sizeBytes = fields["size"]?.toLongOrNull()?.takeIf { it > 0 },
                imageHash = fields["hash"]?.takeIf { it.isNotBlank() }?.take(16),
                slot = fields["slot"]?.takeIf { it.isNotBlank() }?.take(16),
                reportedAt = OffsetDateTime.now(),
            ),
        )
    }
}
