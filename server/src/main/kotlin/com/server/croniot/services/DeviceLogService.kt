package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceEventRepository
import com.server.croniot.data.repositories.DeviceLogRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.mqtt.DeviceLogBatch
import croniot.models.dto.DeviceLogEntryDto
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import javax.inject.Inject

// Which croniot-iot Journal stream a batch/record belongs to (plan
// §11.3's device_log/device_event split) - `wireValue` matches
// croniot::log::Stream's ordinal in the CBOR envelope, `ackName` matches
// the string Uplink::onAck() expects in the JSON ack payload
// ({"stream":"logs"|"events"|"data","upToSeq":N}).  `Data` is
// deliberately absent: it has no wire topic on the device side yet
// (Tanda F), so there is nothing to ingest for it today.
enum class LogStream(val wireValue: Int, val ackName: String) {
    LOGS(0, "logs"),
    EVENTS(1, "events");

    companion object {
        fun fromWireValue(value: Int): LogStream? = entries.find { it.wireValue == value }
    }
}

data class AckResult(val stream: LogStream, val upToSeq: Long)

class DeviceLogService @Inject constructor(
    private val deviceLogRepository: DeviceLogRepository,
    private val deviceEventRepository: DeviceEventRepository,
    private val deviceRepository: DeviceRepository,
) {
    private val logger = KotlinLogging.logger {}

    // Persists whichever table `batch.stream` selects, then returns the
    // seq the device should be told is acked - null if the batch can't
    // be attributed to a real device or a supported stream (dropped,
    // not retried: there is nothing sensible to ack back).
    //
    // Deliberately acks `firstSeq + count - 1` (the batch's own upper
    // bound, taken as the max seq actually present) rather than
    // computing the true max-contiguous-seq across this device's whole
    // history (plan §5 point 6's "el ack devuelve el mayor seq
    // contiguo aceptado"). Journal::ack() on the device already
    // tolerates this simplification - any ack for a stream clears that
    // stream's in-flight slot outright, partial or not (see
    // croniot-iot's Uplink.cpp) - and batches are drained
    // oldest-unacked-first, so in the normal case this IS the
    // contiguous max. A batch lost in transit shows up as the *next*
    // batch's firstSeq not being one past this ack, which is a gap the
    // device's own GapMarker/space-reclamation path already handles,
    // not something this ack needs to detect.
    fun ingestBatch(deviceUuid: String, batch: DeviceLogBatch): AckResult? {
        val deviceId = deviceRepository.getId(deviceUuid)
        if (deviceId == null) {
            logger.warn { "ingestBatch: unknown device $deviceUuid, dropping batch (stream=${batch.stream})" }
            return null
        }

        val stream = LogStream.fromWireValue(batch.stream)
        if (stream == null) {
            logger.warn { "ingestBatch: unsupported stream ${batch.stream} for device $deviceUuid, dropping batch" }
            return null
        }

        val inserted = when (stream) {
            LogStream.LOGS -> deviceLogRepository.insertBatch(deviceId, batch.bootId, batch.records)
            LogStream.EVENTS -> deviceEventRepository.insertBatch(deviceId, batch.bootId, batch.records)
        }
        logger.debug {
            "ingestBatch: device=$deviceUuid stream=${stream.ackName} inserted=$inserted/${batch.records.size} firstSeq=${batch.firstSeq} count=${batch.count}"
        }

        val upToSeq = if (batch.records.isEmpty()) batch.firstSeq else batch.records.maxOf { it.seq }
        return AckResult(stream, upToSeq)
    }

    fun getLogs(
        deviceUuid: String,
        limit: Int,
        before: OffsetDateTime?,
        beforeId: Long?,
        minLevel: Int?,
    ): List<DeviceLogEntryDto> {
        val deviceId = deviceRepository.getId(deviceUuid) ?: return emptyList()
        return deviceLogRepository.getRecent(deviceId, limit, before, beforeId, minLevel)
    }

    fun getEvents(
        deviceUuid: String,
        limit: Int,
        before: OffsetDateTime?,
        beforeId: Long?,
        minLevel: Int?,
    ): List<DeviceLogEntryDto> {
        val deviceId = deviceRepository.getId(deviceUuid) ?: return emptyList()
        return deviceEventRepository.getRecent(deviceId, limit, before, beforeId, minLevel)
    }
}
