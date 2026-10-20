package com.server.croniot.services

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

data class FirmwareImageInfo(
    val project: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
)

// Reads what an ESP-IDF app image says about itself, so an upload can't be
// labelled with a project or version it isn't. Layout (ESP-IDF 5.x):
// a 24-byte image header starting with 0xE9, an 8-byte header for the first
// segment, and then esp_app_desc_t, which begins with 0xABCD5432 and has
// `version` at +16 and `project_name` at +48 (32 bytes each, NUL-padded).
object FirmwareImage {
    // The ESP32-C6 maps at most 16 MB of flash: no app image is larger.
    const val MAX_SIZE_BYTES = 16 * 1024 * 1024

    private const val IMAGE_MAGIC = 0xE9
    private const val APP_DESC_OFFSET = 32
    private const val APP_DESC_MAGIC = 0xABCD5432.toInt()
    private const val VERSION_OFFSET = APP_DESC_OFFSET + 16
    private const val PROJECT_OFFSET = APP_DESC_OFFSET + 48
    private const val FIELD_LEN = 32

    fun inspect(image: ByteArray): FirmwareImageInfo? {
        if (image.size < PROJECT_OFFSET + FIELD_LEN || image.size > MAX_SIZE_BYTES) return null
        if ((image[0].toInt() and 0xFF) != IMAGE_MAGIC) return null
        val magic = ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN).getInt(APP_DESC_OFFSET)
        if (magic != APP_DESC_MAGIC) return null

        val version = cString(image, VERSION_OFFSET) ?: return null
        val project = cString(image, PROJECT_OFFSET) ?: return null
        return FirmwareImageInfo(project, version, image.size.toLong(), sha256Hex(image))
    }

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    // Printable ASCII only: these end up in an MQTT payload, NVS and logs.
    private fun cString(image: ByteArray, offset: Int): String? {
        val end = (offset until offset + FIELD_LEN).firstOrNull { image[it].toInt() == 0 } ?: return null
        if (end == offset) return null
        val bytes = image.copyOfRange(offset, end)
        if (bytes.any { it < 0x21 || it > 0x7e }) return null
        return String(bytes, Charsets.US_ASCII)
    }
}
