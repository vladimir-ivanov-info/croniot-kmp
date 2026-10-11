package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceLogConfigRepository
import com.server.croniot.data.repositories.DeviceRepository
import croniot.models.Result
import javax.inject.Inject
import kotlinx.serialization.json.Json

class DeviceLogConfigService @Inject constructor(
    private val deviceLogConfigRepository: DeviceLogConfigRepository,
    private val deviceRepository: DeviceRepository,
) {

    // Only checks the config is *valid JSON* (an object, specifically -
    // croniot-iot's own RemoteLogConfig parser is the actual authority
    // on the shape, and it already rejects a malformed config safely
    // rather than half-applying it - see that SDK's Log.h). Storing and
    // republishing something the device will just reject is harmless;
    // storing something that isn't JSON at all isn't worth persisting.
    fun setConfig(deviceUuid: String, configJson: String): Result {
        val deviceId = deviceRepository.getId(deviceUuid)
            ?: return Result(false, "Unknown device $deviceUuid")

        val looksLikeJsonObject = runCatching { Json.parseToJsonElement(configJson) }
            .map { it is kotlinx.serialization.json.JsonObject }
            .getOrDefault(false)
        if (!looksLikeJsonObject) return Result(false, "log_config must be a JSON object")

        deviceLogConfigRepository.upsert(deviceId, configJson)
        return Result(true, "log_config stored")
    }

    fun getConfig(deviceUuid: String): String? {
        val deviceId = deviceRepository.getId(deviceUuid) ?: return null
        return deviceLogConfigRepository.get(deviceId)
    }
}
