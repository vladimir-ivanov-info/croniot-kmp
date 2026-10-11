package com.server.croniot.cbor

// Minimal CBOR (RFC 8949) decoder for exactly the subset croniot-iot's
// hand-rolled encoder emits (see croniot-iot's CborWriter.h/
// BatchEnvelope.h/SensorBatchEncoder.h): definite-length unsigned
// integers (major type 0), definite-length text strings (major type
// 3), definite-length arrays (major type 4), and IEEE 754 double-
// precision floats (major type 7, additional info 27 - sensor readings
// only; log/event records never needed a real number type). Hand-rolled to match that
// encoder symmetrically rather than pulling in a general CBOR library:
// kotlinx-serialization-cbor has no clean way to decode a plain
// positional array-of-arrays into named fields (it encodes classes as
// CBOR maps, not arrays), and Jackson CBOR would work but is a whole new
// dependency family for one narrow, already golden-tested wire shape
// (see croniot-iot's BatchEnvelopeTest.cpp) that will never grow beyond
// these three CBOR types on this side either.
class CborReader(private val bytes: ByteArray) {
    private var pos = 0

    fun readUInt(): Long {
        val initial = nextByte()
        val majorType = initial ushr 5
        require(majorType == 0) { "expected unsigned int (major 0) at byte $pos, got major $majorType" }
        return readLength(initial and 0x1F)
    }

    fun readArrayHeader(): Int {
        val initial = nextByte()
        val majorType = initial ushr 5
        require(majorType == 4) { "expected array (major 4) at byte $pos, got major $majorType" }
        return readLength(initial and 0x1F).toInt()
    }

    fun readTextString(): String {
        val initial = nextByte()
        val majorType = initial ushr 5
        require(majorType == 3) { "expected text string (major 3) at byte $pos, got major $majorType" }
        val len = readLength(initial and 0x1F).toInt()
        require(pos + len <= bytes.size) { "text string length $len at byte $pos exceeds buffer" }
        val str = String(bytes, pos, len, Charsets.UTF_8)
        pos += len
        return str
    }

    fun readDouble(): Double {
        val initial = nextByte()
        require(initial == 0xFB) { "expected float64 (0xFB) at byte $pos, got 0x${initial.toString(16)}" }
        var bits = 0L
        repeat(8) { bits = (bits shl 8) or (nextByte().toLong() and 0xFF) }
        return Double.fromBits(bits)
    }

    fun hasMore(): Boolean = pos < bytes.size

    private fun nextByte(): Int {
        require(pos < bytes.size) { "unexpected end of CBOR buffer at byte $pos" }
        return (bytes[pos++].toInt() and 0xFF)
    }

    private fun readLength(argument: Int): Long {
        return when (argument) {
            in 0..23 -> argument.toLong()
            24 -> nextByte().toLong()
            25 -> {
                val hi = nextByte()
                val lo = nextByte()
                ((hi.toLong() shl 8) or lo.toLong())
            }
            26 -> {
                var v = 0L
                repeat(4) { v = (v shl 8) or nextByte().toLong() }
                v
            }
            27 -> {
                var v = 0L
                repeat(8) { v = (v shl 8) or nextByte().toLong() }
                v
            }
            else -> error("unsupported CBOR length encoding: argument=$argument (indefinite length is not produced by this format)")
        }
    }
}
