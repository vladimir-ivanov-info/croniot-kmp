package com.server.croniot.controllers

import com.server.croniot.application.DomainException
import com.server.croniot.mqtt.MqttController
import com.server.croniot.services.DeviceLogConfigService
import croniot.models.errors.DomainError
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import javax.inject.Inject

class DeviceLogConfigController @Inject constructor(
    private val deviceLogConfigService: DeviceLogConfigService,
) {

    // Body is the raw config JSON, not a typed DTO - its shape is
    // croniot-iot's RemoteLogConfig contract (see that SDK's
    // RemoteLogConfig.h), which this server validates only loosely
    // (setConfig() checks "is this a JSON object", not the exact
    // fields) and otherwise treats as opaque cargo to store and
    // republish. Retained MQTT publish only happens after the store
    // succeeds - a device that's offline right now still gets the
    // config the moment it reconnects, via the retained flag, without
    // this endpoint needing to know or care whether it's currently connected.
    suspend fun putConfig(call: ApplicationCall) {
        val deviceUuid = call.parameters["uuid"]
            ?: throw DomainException(DomainError.Validation("uuid", "Missing uuid"))
        val configJson = call.receiveText()

        val result = deviceLogConfigService.setConfig(deviceUuid, configJson)
        if (result.success) {
            MqttController.publishLogConfig(deviceUuid, configJson)
        }
        call.respond(result)
    }

    suspend fun getConfig(call: ApplicationCall) {
        val deviceUuid = call.parameters["uuid"]
            ?: throw DomainException(DomainError.Validation("uuid", "Missing uuid"))

        val configJson = deviceLogConfigService.getConfig(deviceUuid)
        if (configJson == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respondText(configJson, ContentType.Application.Json)
        }
    }
}
