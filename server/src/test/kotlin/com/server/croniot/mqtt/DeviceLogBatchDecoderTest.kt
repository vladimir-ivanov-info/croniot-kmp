package com.server.croniot.mqtt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceLogBatchDecoderTest {

    // Same golden bytes as croniot-iot's BatchEnvelopeTest.cpp
    // (MatchesIndependentCborGoldenBytes), independently generated there
    // with Python's cbor2 from [7,0,1,2,[[1,1000,0,"TAG","hello"],
    // [2,2000,1,"TAG","world"]]] - decoding the exact same bytes here
    // confirms this decoder is byte-compatible with the device's real
    // encoder without needing hardware or a live MQTT round trip.
    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte() }

    @Test
    fun `WHEN decoding the golden batch bytes THEN every field matches the independently-encoded source`() {
        val golden = "85070001028285011903e800635441476568656c6c6f85021907d0016354414765776f726c64"
        val batch = DeviceLogBatchDecoder.decode(hexToBytes(golden))

        assertEquals(7L, batch.bootId)
        assertEquals(0, batch.stream)
        assertEquals(1L, batch.firstSeq)
        assertEquals(2L, batch.count)
        assertEquals(2, batch.records.size)

        assertEquals(1L, batch.records[0].seq)
        assertEquals(1000L, batch.records[0].uptimeMs)
        assertEquals(0, batch.records[0].level)
        assertEquals("TAG", batch.records[0].tag)
        assertEquals("hello", batch.records[0].message)

        assertEquals(2L, batch.records[1].seq)
        assertEquals(2000L, batch.records[1].uptimeMs)
        assertEquals(1, batch.records[1].level)
        assertEquals("TAG", batch.records[1].tag)
        assertEquals("world", batch.records[1].message)
    }

    @Test
    fun `WHEN the batch has zero records THEN decode returns an empty list without error`() {
        // [7, 0, 1, 0, []]
        val bytes = hexToBytes("850700010080")
        val batch = DeviceLogBatchDecoder.decode(bytes)
        assertEquals(0L, batch.count)
        assertEquals(emptyList<DeviceLogRecord>(), batch.records)
    }

    @Test
    fun `WHEN a value needs the 2-byte-length encoding THEN readUInt decodes it correctly`() {
        // count=300 forces CBOR's uint16 length form (argument 25, two
        // length bytes) instead of the 1-byte forms exercised above.
        val bytes = hexToBytes("85") + // array(5)
            hexToBytes("00") + // bootId = 0
            hexToBytes("00") + // stream = 0
            hexToBytes("00") + // firstSeq = 0
            byteArrayOf(0x19.toByte(), 0x01.toByte(), 0x2C.toByte()) + // count = 300 (uint16)
            hexToBytes("80") // records = []
        val batch = DeviceLogBatchDecoder.decode(bytes)
        assertEquals(300L, batch.count)
    }
}
