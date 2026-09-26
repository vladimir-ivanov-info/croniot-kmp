package com.server.croniot.mqtt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SensorBatchEnvelopeDecoderTest {

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte() }

    // Golden bytes independently generated with Python's struct module
    // from a batch envelope [7, 2, 5, 1, [[42, 1000, 60000,
    // [21.5, 22.0, 21.8]]]] - bootId=7, stream=2 (Data), firstSeq=5,
    // count=1, one sensor record for sensorUid=42.
    @Test
    fun `WHEN decoding the golden envelope bytes THEN every field matches the independently-encoded source`() {
        val golden = "85070205018184182a1903e819ea6083fb4035800000000000fb4036000000000000fb4035cccccccccccd"
        val envelope = SensorBatchEnvelopeDecoder.decode(hexToBytes(golden))

        assertEquals(7L, envelope.bootId)
        assertEquals(2, envelope.stream)
        assertEquals(1, envelope.records.size)

        val record = envelope.records[0]
        assertEquals(5L, record.seq)  // firstSeq (5) + index (0)
        assertEquals(42, record.sensorUid)
        assertEquals(1000L, record.t0Ms)
        assertEquals(60000L, record.dtMs)
        assertEquals(listOf(21.5, 22.0, 21.8), record.values)
    }

    @Test
    fun `WHEN a batch has multiple records THEN seq increments per record from firstSeq`() {
        // [7, 2, 100, 2, [[1,0,0,[]], [2,0,0,[]]]]
        val bytes = hexToBytes("85") + hexToBytes("07") + hexToBytes("02") +
            byteArrayOf(0x18.toByte(), 100) + hexToBytes("02") + hexToBytes("82") +
            hexToBytes("8401000080") + hexToBytes("8402000080")
        val envelope = SensorBatchEnvelopeDecoder.decode(bytes)

        assertEquals(2, envelope.records.size)
        assertEquals(100L, envelope.records[0].seq)
        assertEquals(101L, envelope.records[1].seq)
    }
}
