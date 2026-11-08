package com.server.croniot.services

import com.server.croniot.application.DomainException
import com.server.croniot.data.db.daos.DeviceFirmware
import com.server.croniot.data.db.daos.DeviceOtaRequest
import com.server.croniot.data.db.daos.OtaUpdate
import com.server.croniot.data.db.daos.FirmwareRelease
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.FirmwareRepository
import com.server.croniot.ota.OtaEvents
import com.server.croniot.testsupport.FirmwareImages
import croniot.models.errors.DomainError
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime

class OtaServiceTest {

    private val firmwareRepository: FirmwareRepository = mockk(relaxUnitFun = true)
    private val deviceRepository: DeviceRepository = mockk()
    private val deviceTokenRepository: DeviceTokenRepository = mockk()
    private val service = OtaService(firmwareRepository, deviceRepository, deviceTokenRepository)

    init {
        // A device that hasn't reported its firmware and has no update open.
        every { firmwareRepository.getFirmware(any()) } returns null
        every { firmwareRepository.latestOpenUpdate(any()) } returns null
    }

    private val release = FirmwareRelease(
        id = 12,
        project = "croniot-watering-system",
        version = "1.2.0",
        sizeBytes = 4096,
        sha256 = "a".repeat(64),
        createdAt = OffsetDateTime.now(),
    )

    @Test
    fun `WHEN an app image is uploaded THEN its own project and version are stored`() {
        val image = FirmwareImages.fake(version = "1.2.0")
        every { firmwareRepository.insertRelease(any(), image) } returns 12L
        every { firmwareRepository.getRelease(12L) } returns release

        val stored = service.upload(image)

        assertEquals(release, stored.release)
        assertTrue(stored.created)
        verify {
            firmwareRepository.insertRelease(
                match { it.version == "1.2.0" && it.project == "croniot-watering-system" },
                image
            )
        }
    }

    @Test
    fun `WHEN the upload is not an app image THEN it is a validation error`() {
        val error = assertThrows<DomainException> { service.upload(ByteArray(100)) }
        assertTrue(error.error is DomainError.Validation)
    }

    @Test
    fun `WHEN the same version is uploaded with different content THEN it is a conflict`() {
        every { firmwareRepository.insertRelease(any(), any()) } returns null
        every { firmwareRepository.findRelease("croniot-watering-system", "1.2.0") } returns release
        val error = assertThrows<DomainException> { service.upload(FirmwareImages.fake()) }
        assertTrue(error.error is DomainError.Conflict)
    }

    @Test
    fun `WHEN the very same file is uploaded again THEN the existing release comes back`() {
        val image = FirmwareImages.fake()
        val sameFile = release.copy(sha256 = FirmwareImage.sha256Hex(image))
        every { firmwareRepository.insertRelease(any(), any()) } returns null
        every { firmwareRepository.findRelease("croniot-watering-system", "1.2.0") } returns sameFile

        val upload = service.upload(image)

        assertEquals(sameFile, upload.release)
        assertEquals(false, upload.created)
    }

    private fun installed(project: String = "croniot-watering-system", version: String = "1.1.0") = DeviceFirmware(
        project = project,
        version = version,
        builtAt = null,
        idfVersion = null,
        sizeBytes = null,
        imageHash = "670c0caa323fec74",
        slot = "ota_0",
        reportedAt = OffsetDateTime.now(),
    )

    private fun update(status: String, version: String = "1.1.5") = OtaUpdate(
        id = 3,
        deviceId = 7,
        releaseId = 11,
        requestId = "old",
        version = version,
        fromVersion = "1.1.0",
        status = status,
        progress = 100,
        detail = null,
        requestedAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now(),
        finishedAt = null,
    )

    @Test
    fun `WHEN an update is requested THEN it starts a history row from the installed version`() {
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getRelease(12L) } returns release
        every { firmwareRepository.getFirmware(7L) } returns installed()
        every { firmwareRepository.latestOpenUpdate(7L) } returns update(OtaEvents.DOWNLOADING)
        val requestId = slot<String>()
        every { firmwareRepository.upsertRequest(7L, 12L, capture(requestId)) } returns Unit

        service.requestUpdate("dev", 12L)

        verify { firmwareRepository.finishOpenUpdates(7L, OtaEvents.CANCELLED, any()) }
        verify { firmwareRepository.insertUpdate(7L, 12L, requestId.captured, "1.2.0", "1.1.0") }
    }

    @Test
    fun `WHEN the release is for another project or already installed THEN it is a conflict`() {
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getRelease(12L) } returns release
        every { firmwareRepository.latestOpenUpdate(7L) } returns null

        every { firmwareRepository.getFirmware(7L) } returns installed(project = "other-project")
        assertTrue(assertThrows<DomainException> { service.requestUpdate("dev", 12L) }.error is DomainError.Conflict)

        every { firmwareRepository.getFirmware(7L) } returns installed(version = "1.2.0")
        assertTrue(assertThrows<DomainException> { service.requestUpdate("dev", 12L) }.error is DomainError.Conflict)

        verify(exactly = 0) { firmwareRepository.upsertRequest(any(), any(), any()) }
    }

    @Test
    fun `WHEN the device is installing THEN a new request and a cancel are refused`() {
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getRelease(12L) } returns release
        every { firmwareRepository.getFirmware(7L) } returns installed()
        every { firmwareRepository.latestOpenUpdate(7L) } returns update(OtaEvents.VALIDATING)

        assertTrue(assertThrows<DomainException> { service.requestUpdate("dev", 12L) }.error is DomainError.Conflict)
        assertTrue(assertThrows<DomainException> { service.cancel("dev") }.error is DomainError.Conflict)
        verify(exactly = 0) { firmwareRepository.deleteRequest(any()) }
    }

    @Test
    fun `WHEN the installed image matches an upload THEN the overview links it`() {
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getFirmware(7L) } returns installed(version = "1.2.0")
        every { firmwareRepository.findReleaseByImageHash("670c0caa323fec74") } returns release
        every { firmwareRepository.history(7L, any()) } returns listOf(update(OtaEvents.CONFIRMED, "1.2.0"))

        val o = service.overview("dev")

        assertEquals(release, o.installedRelease)
        assertNull(o.current)
        assertEquals(1, o.history.size)
    }

    @Test
    fun `WHEN an update is requested THEN the manifest points at the release on this server`() {
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getRelease(12L) } returns release
        val requestId = slot<String>()
        every { firmwareRepository.upsertRequest(7L, 12L, capture(requestId)) } returns Unit

        val manifest = service.requestUpdate("dev", 12L)

        assertEquals("1.2.0", manifest.version)
        assertEquals("/api/iot/firmware/12", manifest.path)
        assertEquals(4096L, manifest.size)
        assertEquals(release.sha256, manifest.sha256)
        assertEquals(requestId.captured, manifest.requestId)
    }

    @Test
    fun `WHEN the same release is requested again THEN the request id changes`() {
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getRelease(12L) } returns release

        val first = service.requestUpdate("dev", 12L)
        val second = service.requestUpdate("dev", 12L)

        assertNotEquals(first.requestId, second.requestId)
    }

    @Test
    fun `WHEN the device or the release does not exist THEN it is not found`() {
        every { deviceRepository.getId("ghost") } returns null
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getRelease(99L) } returns null

        assertTrue(assertThrows<DomainException> { service.requestUpdate("ghost", 12L) }.error is DomainError.NotFound)
        assertTrue(assertThrows<DomainException> { service.requestUpdate("dev", 99L) }.error is DomainError.NotFound)
    }

    @Test
    fun `WHEN there is no pending request THEN currentRequest is null`() {
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getRequest(7L) } returns null
        assertNull(service.currentRequest("dev"))
    }

    @Test
    fun `WHEN the device asks for the release it was offered THEN it gets the image`() {
        val image = FirmwareImages.fake()
        every { deviceTokenRepository.isTokenCorrect("dev", "tok") } returns true
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getRequest(7L) } returns DeviceOtaRequest(7L, 12L, "r", OffsetDateTime.now())
        every { firmwareRepository.getImage(12L) } returns image

        assertArrayEquals(image, service.imageForDevice("dev", "tok", 12L))
    }

    @Test
    fun `WHEN the device credentials are missing or wrong THEN the download is refused`() {
        every { deviceTokenRepository.isTokenCorrect("dev", "bad") } returns false

        assertTrue(
            assertThrows<DomainException> { service.imageForDevice(null, "tok", 12L) }.error is DomainError.Unauthorized
        )
        assertTrue(
            assertThrows<DomainException> { service.imageForDevice("dev", null, 12L) }.error is DomainError.Unauthorized
        )
        assertTrue(assertThrows<DomainException> {
            service.imageForDevice("dev", "bad", 12L)
        }.error is DomainError.Unauthorized)
    }

    @Test
    fun `WHEN the device asks for a release it was not offered THEN it is not found`() {
        every { deviceTokenRepository.isTokenCorrect("dev", "tok") } returns true
        every { deviceRepository.getId("dev") } returns 7L
        every { firmwareRepository.getRequest(7L) } returns DeviceOtaRequest(7L, 12L, "r", OffsetDateTime.now())

        val error = assertThrows<DomainException> { service.imageForDevice("dev", "tok", 13L) }
        assertTrue(error.error is DomainError.NotFound)
        verify(exactly = 0) { firmwareRepository.getImage(any()) }
    }
}
