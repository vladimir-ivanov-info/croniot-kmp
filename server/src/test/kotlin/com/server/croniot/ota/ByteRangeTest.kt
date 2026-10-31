package com.server.croniot.ota

import com.server.croniot.ota.ByteRange.Companion.Parsed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ByteRangeTest {

    @Test
    fun `WHEN there is no Range header THEN the whole image is served`() {
        assertEquals(Parsed.Whole, ByteRange.parse(null, 100))
    }

    @Test
    fun `WHEN the range is open-ended THEN it runs to the last byte`() {
        assertEquals(Parsed.Partial(ByteRange(40, 99)), ByteRange.parse("bytes=40-", 100))
    }

    @Test
    fun `WHEN the range has an end past the image THEN it is clamped`() {
        assertEquals(Parsed.Partial(ByteRange(10, 99)), ByteRange.parse("bytes=10-500", 100))
        assertEquals(Parsed.Partial(ByteRange(0, 9)), ByteRange.parse("bytes=0-9", 100))
    }

    @Test
    fun `WHEN the range starts past the image THEN it is unsatisfiable`() {
        assertEquals(Parsed.Unsatisfiable, ByteRange.parse("bytes=100-", 100))
        assertEquals(Parsed.Unsatisfiable, ByteRange.parse("bytes=50-10", 100))
    }

    @Test
    fun `WHEN the header is something devices do not send THEN the whole image is served`() {
        assertEquals(Parsed.Whole, ByteRange.parse("bytes=-20", 100))
        assertEquals(Parsed.Whole, ByteRange.parse("bytes=0-1,5-6", 100))
        assertEquals(Parsed.Whole, ByteRange.parse("items=0-5", 100))
        assertEquals(Parsed.Whole, ByteRange.parse("bytes=a-b", 100))
    }

    @Test
    fun `WHEN a range is parsed THEN its length counts both ends`() {
        assertEquals(10L, ByteRange(0, 9).length)
    }
}
