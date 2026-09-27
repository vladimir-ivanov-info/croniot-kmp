package com.server.croniot.mqtt

import com.server.croniot.services.DeviceLogService
import io.github.oshai.kotlinlogging.KotlinLogging

// One instance per subscribed prefix (logs/events - see
// MqttController.initDeviceLogController()), sharing the same
// DeviceLogService: `ingestBatch()` already dispatches to the right
// table via the envelope's own `stream` field, so the two prefixes only
// differ in how the device UUID is parsed out of the topic.
//
// Deliberately NOT a `croniot.models.MqttDataProcessor` (the shared
// interface's `process(topic, data: Any)` is non-suspend, forcing
// MqttHandler-based callers into either blocking or fire-and-forget for
// the ack publish below) - BinaryMqttSubscriber already runs this inside
// its own coroutine, so `process` here is suspend all the way through to
// MqttController.sendLogAck() instead.
class MqttDataProcessorDeviceLog(
    private val topicPrefix: String,
    private val deviceLogService: DeviceLogService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun process(topic: String, payload: ByteArray) {
        val deviceUuid = topic.removePrefix(topicPrefix)
        try {
            val batch = DeviceLogBatchDecoder.decode(payload)
            val ack = deviceLogService.ingestBatch(deviceUuid, batch) ?: return
            MqttController.sendLogAck(deviceUuid, ack.stream, ack.upToSeq)
        } catch (e: Exception) {
            logger.error(e) { "Error processing device log/event batch from topic $topic (device=$deviceUuid)" }
        }
    }
}
