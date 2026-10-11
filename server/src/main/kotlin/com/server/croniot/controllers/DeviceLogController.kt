package com.server.croniot.controllers

import com.server.croniot.application.DomainException
import com.server.croniot.services.DeviceLogService
import croniot.models.errors.DomainError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class DeviceLogController @Inject constructor(
    private val deviceLogService: DeviceLogService,
) {

    suspend fun getLogs(call: ApplicationCall) {
        val (deviceUuid, limit, before, beforeId, minLevel) = parseCommonParams(call)
        call.respond(deviceLogService.getLogs(deviceUuid, limit, before, beforeId, minLevel))
    }

    suspend fun getEvents(call: ApplicationCall) {
        val (deviceUuid, limit, before, beforeId, minLevel) = parseCommonParams(call)
        call.respond(deviceLogService.getEvents(deviceUuid, limit, before, beforeId, minLevel))
    }

    private data class CommonParams(
        val deviceUuid: String,
        val limit: Int,
        val before: OffsetDateTime?,
        val beforeId: Long?,
        val minLevel: Int?,
    )

    private fun parseCommonParams(call: ApplicationCall): CommonParams {
        val deviceUuid = call.parameters["uuid"]
            ?: throw DomainException(DomainError.Validation("uuid", "Missing uuid"))

        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        val before = parseBefore(call.request.queryParameters["before"])
        val beforeId = call.request.queryParameters["beforeId"]?.toLongOrNull()
        val minLevel = parseLevel(call.request.queryParameters["level"])

        return CommonParams(deviceUuid, limit, before, beforeId, minLevel)
    }

    private fun parseBefore(raw: String?): OffsetDateTime? {
        if (raw == null) return null
        return raw.toLongOrNull()?.let {
            Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC)
        } ?: runCatching { OffsetDateTime.parse(raw) }.getOrNull()
    }

    // Accepts either the plan's string form ("warn") or a raw ordinal
    // ("1") - matches croniot-iot's Level.h naming (Error < Warn < Info
    // < Debug < Trace, case-insensitive here since this is a query
    // param, not the wire format).
    private fun parseLevel(raw: String?): Int? {
        if (raw == null) return null
        raw.toIntOrNull()?.let { return it }
        return when (raw.lowercase()) {
            "error" -> 0
            "warn" -> 1
            "info" -> 2
            "debug" -> 3
            "trace" -> 4
            else -> throw DomainException(DomainError.Validation("level", "Unknown level '$raw'"))
        }
    }
}
