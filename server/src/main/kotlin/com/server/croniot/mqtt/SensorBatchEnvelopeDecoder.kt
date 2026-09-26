package com.server.croniot.mqtt

import com.server.croniot.cbor.CborReader

// One flush event's worth of samples for one sensor, as decoded off the
// wire - `[sensorUid, t0Ms, dtMs, values[]]` (see croniot-iot's
// Sensors/SensorBatchEncoder.h). `seq` is NOT part of this payload on
// the wire - croniot-iot deliberately doesn't embed it (the *outer*
// batch envelope's `firstSeq`/`count` already identify each frame's
// position), so it's reconstructed here as `envelope.firstSeq + index`
// while decoding - one seq per *flush event* (i.e. per frame/record),
// not per individual sample value.
data class SensorBatchRecord(
    val seq: Long,
    val sensorUid: Int,
    val t0Ms: Long,
    val dtMs: Long,
    val values: List<Double>,
)

data class SensorBatchEnvelope(
    val bootId: Long,
    val stream: Int,
    val records: List<SensorBatchRecord>,
)

// Deliberately a separate decoder from DeviceLogBatchDecoder rather than
// a shared refactor of it, even though the outer envelope shape
// (`[bootId, stream, firstSeq, count, records[]]`) is identical - the
// per-record shape underneath is not (4 numeric-ish fields here vs. 5
// fields with two strings there), and CborReader has no generic "read
// one value of unknown type" - each decoder has to know its own record
// shape up front either way. Duplicating the ~6-line outer-envelope
// parse is cheaper than adding that generality for one caller.
object SensorBatchEnvelopeDecoder {
    fun decode(bytes: ByteArray): SensorBatchEnvelope {
        val reader = CborReader(bytes)

        val outerLen = reader.readArrayHeader()
        require(outerLen == 5) { "expected a 5-element batch envelope [bootId, stream, firstSeq, count, records], got $outerLen elements" }

        val bootId = reader.readUInt()
        val stream = reader.readUInt().toInt()
        val firstSeq = reader.readUInt()
        reader.readUInt()  // count - redundant with records.size, not needed here

        val recordCount = reader.readArrayHeader()
        val records = ArrayList<SensorBatchRecord>(recordCount)
        repeat(recordCount) { index ->
            val fieldCount = reader.readArrayHeader()
            require(fieldCount == 4) { "expected a 4-element sensor record [sensorUid, t0Ms, dtMs, values], got $fieldCount elements" }

            val sensorUid = reader.readUInt().toInt()
            val t0Ms = reader.readUInt()
            val dtMs = reader.readUInt()
            val valueCount = reader.readArrayHeader()
            val values = (0 until valueCount).map { reader.readDouble() }

            records.add(SensorBatchRecord(firstSeq + index, sensorUid, t0Ms, dtMs, values))
        }

        return SensorBatchEnvelope(bootId, stream, records)
    }
}
