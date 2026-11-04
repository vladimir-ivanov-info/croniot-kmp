package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.services.FirmwareImage
import com.server.croniot.testsupport.FirmwareImages
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FirmwareJooqDaoImplIntegrationTest {

    private val dsl = PostgresTestcontainer.dsl
    private val dao = FirmwareJooqDaoImpl(dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    private fun insertDevice(uuid: String = "dev"): Long {
        val accountId = AccountJooqDaoImpl(dsl).insert(
            Account(uuid = "acc-$uuid", nickname = "n", email = "$uuid@example.com", devices = mutableListOf()),
            password = "pwd",
        )
        return DeviceJooqDaoImpl(dsl)
            .insert(DeviceEntity(uuid = uuid, name = "n", description = "d", iot = true, accountId = accountId))
    }

    private fun insertRelease(version: String): Long {
        val image = FirmwareImages.fake(version = version)
        return dao.insertRelease(FirmwareImage.inspect(image)!!, image)!!
    }

    @Test
    fun `WHEN a release is stored THEN its metadata and image read back`() {
        val image = FirmwareImages.fake(version = "1.0.0", size = 10_000)
        val info = FirmwareImage.inspect(image)!!

        val id = dao.insertRelease(info, image)!!
        val release = dao.getRelease(id)!!

        assertEquals("croniot-watering-system", release.project)
        assertEquals("1.0.0", release.version)
        assertEquals(10_000L, release.sizeBytes)
        assertEquals(info.sha256, release.sha256)
        assertArrayEquals(image, dao.getImage(id))
    }

    @Test
    fun `WHEN the same project and version are stored twice THEN the second insert is refused`() {
        insertRelease("1.0.0")
        val again = FirmwareImages.fake(version = "1.0.0", size = 5000)
        assertNull(dao.insertRelease(FirmwareImage.inspect(again)!!, again))
    }

    @Test
    fun `WHEN releases are listed THEN the newest comes first`() {
        insertRelease("1.0.0")
        insertRelease("1.1.0")

        assertEquals(listOf("1.1.0", "1.0.0"), dao.listReleases().map { it.version })
    }

    @Test
    fun `WHEN a device is asked again THEN its single request is replaced`() {
        val deviceId = insertDevice()
        val first = insertRelease("1.0.0")
        val second = insertRelease("1.1.0")

        dao.upsertRequest(deviceId, first, "r1")
        dao.upsertRequest(deviceId, second, "r2")
        val request = dao.getRequest(deviceId)

        assertNotNull(request)
        assertEquals(second, request!!.releaseId)
        assertEquals("r2", request.requestId)
    }

    @Test
    fun `WHEN a request is cancelled THEN it is gone, and cancelling again reports nothing removed`() {
        val deviceId = insertDevice()
        dao.upsertRequest(deviceId, insertRelease("1.0.0"), "r1")

        assertTrue(dao.deleteRequest(deviceId))
        assertNull(dao.getRequest(deviceId))
        assertFalse(dao.deleteRequest(deviceId))
    }
}
