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
}
