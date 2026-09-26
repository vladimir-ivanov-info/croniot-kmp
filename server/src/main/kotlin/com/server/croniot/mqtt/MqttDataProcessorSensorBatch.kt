package com.server.croniot.mqtt

import com.server.croniot.services.SensorBatchService
import io.github.oshai.kotlinlogging.KotlinLogging

// Binary-safe, batch counterpart to the legacy MqttDataProcessorSensor
// (which stays untouched - the old per-reading topic keeps working
// exactly as before, per the plan's own backward-compatibility
// requirement). This one handles the new /iot_to_server/sensor_batch/+
// wildcard - suspend all the way through, same shape as
// MqttDataProcessorDeviceLog, for the same reason (the ack publish
// below is itself suspend).
class MqttDataProcessorSensorBatch(
    private val topicPrefix: String,
    private val sensorBatchService: SensorBatchService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun process(topic: String, payload: ByteArray) {
        val deviceUuid = topic.removePrefix(topicPrefix)
        try {
            val envelope = SensorBatchEnvelopeDecoder.decode(payload)
            val ack = sensorBatchService.ingestBatch(deviceUuid, envelope) ?: return
            MqttController.sendLogAck(deviceUuid, ack.stream, ack.upToSeq)
        } catch (e: Exception) {
            logger.error(e) { "Error processing sensor batch from topic $topic (device=$deviceUuid)" }
        }
    }
}
