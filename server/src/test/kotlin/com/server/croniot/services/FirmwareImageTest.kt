package com.server.croniot.services

import com.server.croniot.testsupport.FirmwareImages
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FirmwareImageTest {

    @Test
    fun `WHEN the image is an ESP-IDF app THEN project, version, size and hash come from it`() {
        val image = FirmwareImages.fake(project = "croniot-watering-system", version = "ota-test-b", size = 5000)

        val info = FirmwareImage.inspect(image)!!

        assertEquals("croniot-watering-system", info.project)
        assertEquals("ota-test-b", info.version)
        assertEquals(5000L, info.sizeBytes)
        assertEquals(FirmwareImage.sha256Hex(image), info.sha256)
        assertEquals(64, info.sha256.length)
    }

    @Test
    fun `WHEN the first byte is not the image magic THEN it is rejected`() {
        val image = FirmwareImages.fake().also { it[0] = 0 }
        assertNull(FirmwareImage.inspect(image))
    }

    @Test
    fun `WHEN the app descriptor magic is missing THEN it is rejected`() {
        val image = FirmwareImages.fake().also { it[32] = 0 }
        assertNull(FirmwareImage.inspect(image))
    }

    @Test
    fun `WHEN the version is empty or not printable THEN it is rejected`() {
        assertNull(FirmwareImage.inspect(FirmwareImages.fake(version = "")))
        assertNull(FirmwareImage.inspect(FirmwareImages.fake(version = "bad version")))
    }

    @Test
    fun `WHEN the image is too small or too large THEN it is rejected`() {
        assertNull(FirmwareImage.inspect(ByteArray(10)))
        assertNull(FirmwareImage.inspect(FirmwareImages.fake(size = FirmwareImage.MAX_SIZE_BYTES + 1)))
    }

    @Test
    fun `WHEN the image has a build date, IDF version and appended hash THEN they are read too`() {
        val image = FirmwareImages.fake(appendHash = true)
        val info = FirmwareImage.inspect(image)!!

        assertEquals("2026-11-07T19:50:09", info.builtAt)
        assertEquals("v5.5.3", info.idfVersion)
        assertEquals(image.copyOfRange(image.size - 32, image.size).joinToString("") { "%02x".format(it) }, info.imageHash)
    }

    @Test
    fun `WHEN the header says no hash or the tail is not the hash THEN there is no image hash`() {
        assertEquals(null, FirmwareImage.inspect(FirmwareImages.fake(appendHash = false))!!.imageHash)
        val broken = FirmwareImages.fake(appendHash = true).also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertEquals(null, FirmwareImage.inspect(broken)!!.imageHash)
    }

    @Test
    fun `WHEN the build date does not parse THEN builtAt is null`() {
        assertEquals("2027-01-31T00:00:05", FirmwareImage.isoBuildTime("Jan 31 2027", "00:00:05"))
        assertEquals(null, FirmwareImage.isoBuildTime("Foo  7 2026", "19:50:09"))
        assertEquals(null, FirmwareImage.isoBuildTime("Nov  7 2026", "late"))
        assertEquals(null, FirmwareImage.isoBuildTime(null, null))
    }
}
