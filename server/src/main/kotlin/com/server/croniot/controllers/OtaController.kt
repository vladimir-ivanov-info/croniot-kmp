package com.server.croniot.controllers

import com.server.croniot.application.DomainException
import com.server.croniot.data.db.daos.FirmwareRelease
import com.server.croniot.ota.ByteRange
import com.server.croniot.ota.FirmwareReleaseDto
import com.server.croniot.ota.OtaManifestDto
import com.server.croniot.ota.OtaRequestPublisher
import com.server.croniot.ota.OtaUpdateRequestDto
import com.server.croniot.services.FirmwareImage
import com.server.croniot.services.OtaService
import croniot.models.errors.DomainError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentLength
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import kotlinx.serialization.json.Json
import javax.inject.Inject

//   POST   /api/admin/firmware              body: the app .bin (admin)
//   GET    /api/admin/firmware              uploaded releases (admin)
//   PUT    /api/admin/devices/{uuid}/ota    {releaseId}: ask the device to install it (admin)
//   GET    /api/admin/devices/{uuid}/ota    the pending request, 404 if none (admin)
//   DELETE /api/admin/devices/{uuid}/ota    cancel the request (admin)
//   GET    /api/iot/firmware/{id}           the image, for the device that was asked
//                                           (X-Device-Uuid + X-Device-Token, Range supported)
class OtaController @Inject constructor(
    private val otaService: OtaService,
    private val publisher: OtaRequestPublisher,
) {

    suspend fun uploadFirmware(call: ApplicationCall) {
        val declared = call.request.contentLength()
        if (declared != null && declared > FirmwareImage.MAX_SIZE_BYTES) {
            throw DomainException(DomainError.Validation("image", "Larger than 16 MB"))
        }
        val image = call.receive<ByteArray>()
        call.respond(HttpStatusCode.Created, otaService.upload(image).toDto())
    }

    suspend fun listFirmware(call: ApplicationCall) {
        call.respond(otaService.listReleases().map { it.toDto() })
    }

    suspend fun requestUpdate(call: ApplicationCall) {
        val uuid = uuid(call)
        val body = call.receive<OtaUpdateRequestDto>()
        val manifest = otaService.requestUpdate(uuid, body.releaseId)
        // Compact, not MessageFactory.json (pretty-printed): this is what the
        // device parses, and it is retained on the broker.
        publisher.publish(uuid, Json.encodeToString(OtaManifestDto.serializer(), manifest))
        call.respond(manifest)
    }

    suspend fun getUpdate(call: ApplicationCall) {
        val manifest = otaService.currentRequest(uuid(call))
        if (manifest == null) call.respond(HttpStatusCode.NotFound) else call.respond(manifest)
    }

    suspend fun cancelUpdate(call: ApplicationCall) {
        val uuid = uuid(call)
        otaService.cancel(uuid)
        // Cleared on the broker even if the database had no request: a
        // stale retained message is exactly what cancelling must remove.
        publisher.publish(uuid, "")
        call.respond(HttpStatusCode.NoContent)
    }

    suspend fun downloadFirmware(call: ApplicationCall) {
        val releaseId = call.parameters["id"]?.toLongOrNull()
            ?: throw DomainException(DomainError.Validation("id", "Invalid firmware id"))
        val image = otaService.imageForDevice(
            deviceUuid = call.request.headers[HEADER_DEVICE_UUID],
            deviceToken = call.request.headers[HEADER_DEVICE_TOKEN],
            releaseId = releaseId,
        )
        val total = image.size.toLong()
        call.response.header(HttpHeaders.AcceptRanges, "bytes")
        when (val parsed = ByteRange.parse(call.request.headers[HttpHeaders.Range], total)) {
            ByteRange.Companion.Parsed.Whole ->
                call.respondBytes(image, ContentType.Application.OctetStream)
            ByteRange.Companion.Parsed.Unsatisfiable -> {
                call.response.header(HttpHeaders.ContentRange, "bytes */$total")
                call.respond(HttpStatusCode.RequestedRangeNotSatisfiable)
            }
            is ByteRange.Companion.Parsed.Partial -> {
                val r = parsed.range
                call.response.header(HttpHeaders.ContentRange, "bytes ${r.start}-${r.endInclusive}/$total")
                call.respondBytes(
                    image.copyOfRange(r.start.toInt(), r.endInclusive.toInt() + 1),
                    ContentType.Application.OctetStream,
                    HttpStatusCode.PartialContent,
                )
            }
        }
    }

    private fun uuid(call: ApplicationCall) = call.parameters["uuid"]
        ?: throw DomainException(DomainError.Validation("uuid", "Missing uuid"))

    private fun FirmwareRelease.toDto() = FirmwareReleaseDto(
        id = id,
        project = project,
        version = version,
        sizeBytes = sizeBytes,
        sha256 = sha256,
        createdAt = createdAt.toString(),
    )

    companion object {
        const val HEADER_DEVICE_UUID = "X-Device-Uuid"
        const val HEADER_DEVICE_TOKEN = "X-Device-Token"
    }
}
