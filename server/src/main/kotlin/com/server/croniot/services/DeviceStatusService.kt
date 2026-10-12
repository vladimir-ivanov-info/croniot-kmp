package com.server.croniot.services

import com.server.croniot.data.db.daos.DeviceStatusDao
import com.server.croniot.data.repositories.DeviceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject

// Keeps device_status in sync with /iot_to_server/status/<uuid>. The
// payload is either the broker's LWT "offline" or the device's birth
// {"boot":N} (croniot-iot WifiMqttController::publishBirth). Each change is
// logged, so it also shows up in the admin live viewer.
class DeviceStatusService @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val deviceStatusDao: DeviceStatusDao,
) {
    private val logger = KotlinLogging.logger {}

    sealed interface Status {
        data object Offline : Status
        data class Online(val bootId: Long?) : Status
    }

    fun onStatusMessage(deviceUuid: String, payload: String) {
        val status = parse(payload)
        if (status == null) {
            logger.warn { "Ignoring unrecognised status for device $deviceUuid: $payload" }
            return
        }
        val deviceId = deviceRepository.getId(deviceUuid) ?: run {
            logger.debug { "Status for unknown device $deviceUuid, ignoring" }
            return
        }
        when (status) {
            Status.Offline -> {
                deviceStatusDao.upsert(deviceId, online = false, bootId = null)
                logger.info { "Device $deviceUuid is offline" }
            }
            is Status.Online -> {
                deviceStatusDao.upsert(deviceId, online = true, bootId = status.bootId)
                logger.info { "Device $deviceUuid is online (boot ${status.bootId ?: "?"})" }
            }
        }
    }

    companion object {
        fun parse(payload: String): Status? {
            val text = payload.trim().trimEnd('\u0000')
            if (text.equals("offline", ignoreCase = true)) return Status.Offline
            val json = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
            return Status.Online(json["boot"]?.jsonPrimitive?.longOrNull)
        }
    }
}
