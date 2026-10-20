package com.server.croniot.testsupport

import java.nio.ByteBuffer
import java.nio.ByteOrder

// A byte array laid out like the start of an ESP-IDF app image: enough for
// FirmwareImage.inspect(), nothing a real device could boot.
object FirmwareImages {
    fun fake(project: String = "croniot-watering-system", version: String = "1.2.0", size: Int = 4096): ByteArray {
        val image = ByteArray(size) { (it % 251).toByte() }
        image[0] = 0xE9.toByte()
        ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN).putInt(32, 0xABCD5432.toInt())
        writeField(image, 48, version)
        writeField(image, 80, project)
        return image
    }

    private fun writeField(image: ByteArray, offset: Int, value: String) {
        val bytes = value.toByteArray(Charsets.US_ASCII)
        for (i in 0 until 32) image[offset + i] = if (i < bytes.size) bytes[i] else 0
    }
}
