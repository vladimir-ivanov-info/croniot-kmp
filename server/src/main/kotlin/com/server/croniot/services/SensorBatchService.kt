package com.server.croniot.services

import com.server.croniot.data.db.daos.IngestBatchRow
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.SensorDataRepository
import com.server.croniot.data.repositories.SensorTypeRepository
import com.server.croniot.mqtt.SensorBatchEnvelope
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import javax.inject.Inject

class SensorBatchService @Inject constructor(
    private val sensorDataRepository: SensorDataRepository,
    private val sensorTypeRepository: SensorTypeRepository,
    private val deviceRepository: DeviceRepository,
    private val ingestBatchRecorder: IngestBatchRecorder,
) {
    private val logger = KotlinLogging.logger {}

    // Persists every record's samples, then acks the highest seq seen -
    // same "ack the batch's own upper bound" simplification as
    // DeviceLogService.ingestBatch() (see that class for why). A record
    // whose sensorUid this device has no registered SensorType for is
    // skipped (logged, not fatal) rather than dropping the whole
    // envelope - one unregistered sensor shouldn't block every other
    // reading in the same MQTT publish from landing.
    fun ingestBatch(deviceUuid: String, envelope: SensorBatchEnvelope): AckResult? {
        val deviceId = deviceRepository.getId(deviceUuid)
        if (deviceId == null) {
            logger.warn { "ingestBatch: unknown device $deviceUuid, dropping sensor batch" }
            return null
        }

        val receivedAt = OffsetDateTime.now()
        var totalInserted = 0
        var totalSamples = 0
        var skippedSamples = 0
        for (record in envelope.records) {
            val sensorTypeId = sensorTypeRepository.getId(deviceId, record.sensorUid.toLong())
            if (sensorTypeId == null) {
                logger.warn { "ingestBatch: device=$deviceUuid has no sensor type uid=${record.sensorUid}, skipping ${record.values.size} sample(s)" }
                skippedSamples += record.values.size
                continue
            }
            totalSamples += record.values.size
            totalInserted += sensorDataRepository.insertBatch(
                deviceId, sensorTypeId, envelope.bootId, record.seq, record.values, receivedAt,
            )
        }
        logger.debug { "ingestBatch: device=$deviceUuid records=${envelope.records.size} inserted=$totalInserted" }

        val upToSeq = envelope.records.maxOfOrNull { it.seq } ?: return null

        // Counted in samples, not records: one record (one device flush)
        // holds many samples, and the unique key that absorbs resends is
        // per sample.
        ingestBatchRecorder.record(
            IngestBatchRow(
                deviceId = deviceId,
                stream = LogStream.DATA.ackName,
                bootId = envelope.bootId,
                firstSeq = envelope.records.minOf { it.seq },
                lastSeq = upToSeq,
                records = totalSamples,
                inserted = totalInserted,
                duplicated = totalSamples - totalInserted,
                skipped = skippedSamples,
            ),
        )
        return AckResult(LogStream.DATA, upToSeq)
    }
}
