package com.server.croniot.testsupport

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

// A byte array laid out like the start of an ESP-IDF app image: enough for
// FirmwareImage.inspect(), nothing a real device could boot.
object FirmwareImages {
    fun fake(
        project: String = "croniot-watering-system",
        version: String = "1.2.0",
        size: Int = 4096,
        date: String = "Nov  7 2026",
        time: String = "19:50:09",
        idf: String = "v5.5.3",
        // Like a real build: the last 32 bytes are the SHA-256 of the rest,
        // and byte 23 of the header says so.
        appendHash: Boolean = false,
    ): ByteArray {
        val image = ByteArray(size) { (it % 251).toByte() }
        image[0] = 0xE9.toByte()
        image[23] = if (appendHash) 1 else 0
        ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN).putInt(32, 0xABCD5432.toInt())
        writeField(image, 48, version, 32)
        writeField(image, 80, project, 32)
        writeField(image, 112, time, 16)
        writeField(image, 128, date, 16)
        writeField(image, 144, idf, 32)
        if (appendHash) {
            val digest = MessageDigest.getInstance("SHA-256").digest(image.copyOfRange(0, size - 32))
            digest.copyInto(image, size - 32)
        }
        return image
    }

    private fun writeField(image: ByteArray, offset: Int, value: String, len: Int) {
        val bytes = value.toByteArray(Charsets.US_ASCII)
        for (i in 0 until len) image[offset + i] = if (i < bytes.size) bytes[i] else 0
    }
}
