package com.server.croniot.mqtt

import com.server.croniot.cbor.CborReader

// One record of croniot-iot's Journal Logs/Events stream, as decoded
// straight off the wire - `[seq, uptimeMs, level, tag, msg]` (see
// croniot-iot's CborWriter.h). `level` stays a raw 0-4 int here rather
// than an enum: the device's `croniot::log::Level` ordering is the
// single source of truth for what these numbers mean, and duplicating
// that mapping into a server-side enum would just be one more place for
// the two sides to drift apart.
data class DeviceLogRecord(
    val seq: Long,
    val uptimeMs: Long,
    val level: Int,
    val tag: String,
    val message: String,
)

// The batch envelope croniot-iot's Uplink actually sends - `[bootId,
// stream, firstSeq, count, records[]]` (see croniot-iot's
// BatchEnvelope.h). `stream` is 0=Logs/1=Events/2=Data, matching
// croniot::log::Stream's ordinal - Data has no wire topic on the device
// side yet (Tanda F), so a decoded batch with stream=2 should never
// actually arrive; DeviceLogBatchDecoder decodes it anyway rather than
// rejecting it, since nothing about the envelope shape depends on which
// stream it is.
data class DeviceLogBatch(
    val bootId: Long,
    val stream: Int,
    val firstSeq: Long,
    val count: Long,
    val records: List<DeviceLogRecord>,
)

object DeviceLogBatchDecoder {
    fun decode(bytes: ByteArray): DeviceLogBatch {
        val reader = CborReader(bytes)

        val outerLen = reader.readArrayHeader()
        require(outerLen == 5) { "expected a 5-element batch envelope [bootId, stream, firstSeq, count, records], got $outerLen elements" }

        val bootId = reader.readUInt()
        val stream = reader.readUInt().toInt()
        val firstSeq = reader.readUInt()
        val count = reader.readUInt()

        val recordCount = reader.readArrayHeader()
        val records = ArrayList<DeviceLogRecord>(recordCount)
        repeat(recordCount) {
            val fieldCount = reader.readArrayHeader()
            require(fieldCount == 5) { "expected a 5-element record [seq, uptimeMs, level, tag, msg], got $fieldCount elements" }

            records.add(
                DeviceLogRecord(
                    seq = reader.readUInt(),
                    uptimeMs = reader.readUInt(),
                    level = reader.readUInt().toInt(),
                    tag = reader.readTextString(),
                    message = reader.readTextString(),
                )
            )
        }

        return DeviceLogBatch(bootId, stream, firstSeq, count, records)
    }
}
