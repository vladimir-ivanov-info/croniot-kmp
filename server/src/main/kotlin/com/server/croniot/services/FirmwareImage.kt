package com.server.croniot.services

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

data class FirmwareImageInfo(
    val project: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
    // "2026-11-07T15:12:16", from the app descriptor's date and time.
    val builtAt: String? = null,
    val idfVersion: String? = null,
    // The SHA-256 ESP-IDF appends to the image, which is what the device
    // reports for the image it runs (firmware_info's `hash`).
    val imageHash: String? = null,
)

// Reads what an ESP-IDF app image says about itself, so an upload can't be
// labelled with a project or version it isn't. Layout (ESP-IDF 5.x):
// a 24-byte image header starting with 0xE9, an 8-byte header for the first
// segment, and then esp_app_desc_t, which begins with 0xABCD5432 and has
// `version` at +16 and `project_name` at +48 (32 bytes each, NUL-padded),
// `time` at +80 and `date` at +96 (16 bytes each) and `idf_ver` at +112.
// Byte 23 of the image header says whether a SHA-256 of the image is
// appended at its end.
object FirmwareImage {
    // The ESP32-C6 maps at most 16 MB of flash: no app image is larger.
    const val MAX_SIZE_BYTES = 16 * 1024 * 1024

    private const val IMAGE_MAGIC = 0xE9
    private const val APP_DESC_OFFSET = 32
    private const val APP_DESC_MAGIC = 0xABCD5432.toInt()
    private const val VERSION_OFFSET = APP_DESC_OFFSET + 16
    private const val PROJECT_OFFSET = APP_DESC_OFFSET + 48
    private const val TIME_OFFSET = APP_DESC_OFFSET + 80
    private const val DATE_OFFSET = APP_DESC_OFFSET + 96
    private const val IDF_OFFSET = APP_DESC_OFFSET + 112
    private const val FIELD_LEN = 32
    private const val SHORT_FIELD_LEN = 16
    private const val HASH_APPENDED_OFFSET = 23
    private const val HASH_LEN = 32
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun inspect(image: ByteArray): FirmwareImageInfo? {
        if (image.size < PROJECT_OFFSET + FIELD_LEN || image.size > MAX_SIZE_BYTES) return null
        if ((image[0].toInt() and 0xFF) != IMAGE_MAGIC) return null
        val magic = ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN).getInt(APP_DESC_OFFSET)
        if (magic != APP_DESC_MAGIC) return null

        val version = cString(image, VERSION_OFFSET) ?: return null
        val project = cString(image, PROJECT_OFFSET) ?: return null
        return FirmwareImageInfo(
            project = project,
            version = version,
            sizeBytes = image.size.toLong(),
            sha256 = sha256Hex(image),
            builtAt = isoBuildTime(text(image, DATE_OFFSET, SHORT_FIELD_LEN), text(image, TIME_OFFSET, SHORT_FIELD_LEN)),
            idfVersion = image.takeIf { it.size >= IDF_OFFSET + FIELD_LEN }?.let { cString(it, IDF_OFFSET) },
            imageHash = appendedHash(image),
        )
    }

    // Only when the header says there is one and it really is the hash of
    // everything before it: a truncated or padded file has no usable hash.
    fun appendedHash(image: ByteArray): String? {
        if (image.size <= HASH_LEN || image[HASH_APPENDED_OFFSET].toInt() != 1) return null
        val body = image.copyOfRange(0, image.size - HASH_LEN)
        val tail = image.copyOfRange(image.size - HASH_LEN, image.size)
        val digest = MessageDigest.getInstance("SHA-256").digest(body)
        return if (digest.contentEquals(tail)) hex(tail) else null
    }

    // "Nov  7 2026" + "15:12:16" (the __DATE__/__TIME__ format) to
    // "2026-11-07T15:12:16"; null if either doesn't parse.
    fun isoBuildTime(date: String?, time: String?): String? {
        val d = date?.trim()?.split(Regex("\\s+")) ?: return null
        if (d.size != 3) return null
        val month = MONTHS.indexOf(d[0]) + 1
        val day = d[1].toIntOrNull() ?: return null
        val year = d[2].toIntOrNull() ?: return null
        if (month == 0 || day !in 1..31) return null
        val t = time?.trim()?.split(":")?.map { it.toIntOrNull() ?: return null } ?: return null
        if (t.size != 3) return null
        return "%04d-%02d-%02dT%02d:%02d:%02d".format(year, month, day, t[0], t[1], t[2])
    }

    private fun text(image: ByteArray, offset: Int, len: Int): String? {
        if (image.size < offset + len) return null
        val end = (offset until offset + len).firstOrNull { image[it].toInt() == 0 } ?: (offset + len)
        return String(image.copyOfRange(offset, end), Charsets.US_ASCII)
    }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    fun sha256Hex(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    // Printable ASCII only: these end up in an MQTT payload, NVS and logs.
    private fun cString(image: ByteArray, offset: Int): String? {
        val end = (offset until offset + FIELD_LEN).firstOrNull { image[it].toInt() == 0 } ?: return null
        if (end == offset) return null
        val bytes = image.copyOfRange(offset, end)
        if (bytes.any { it < 0x21 || it > 0x7e }) return null
        return String(bytes, Charsets.US_ASCII)
    }
}
