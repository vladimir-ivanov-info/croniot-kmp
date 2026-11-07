package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.services.FirmwareImage
import com.server.croniot.testsupport.FirmwareImages
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class OtaHistoryJooqDaoImplIntegrationTest {

    private val dsl = PostgresTestcontainer.dsl
    private val dao = OtaHistoryJooqDaoImpl(dsl)
    private val firmwareDao = FirmwareJooqDaoImpl(dsl)

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
        val image = FirmwareImages.fake(version = version, appendHash = true)
        return firmwareDao.insertRelease(FirmwareImage.inspect(image)!!, image)!!
    }

    @Test
    fun `WHEN an update moves through its states THEN the row follows and finishes once`() {
        val device = insertDevice()
        val release = insertRelease("1.2.0")
        dao.insertUpdate(device, release, "r1", "1.2.0", null)

        val open = dao.latestOpen(device)!!
        assertEquals("requested", open.status)

        dao.updateState(open.id, "downloading", 40, null, finished = false, fromVersion = "1.1.0")
        dao.updateState(open.id, "confirmed", 100, null, finished = true, fromVersion = "ignored")

        val done = dao.findByRequestId("r1")!!
        assertEquals("confirmed", done.status)
        assertEquals(100, done.progress)
        // The first version reported wins.
        assertEquals("1.1.0", done.fromVersion)
        assertNotNull(done.finishedAt)
        assertNull(dao.latestOpen(device))
    }

    @Test
    fun `WHEN a new request replaces open ones THEN only the unfinished rows are closed`() {
        val device = insertDevice()
        val release = insertRelease("1.2.0")
        dao.insertUpdate(device, release, "done", "1.2.0", null)
        dao.updateState(dao.findByRequestId("done")!!.id, "failed", 10, "x", finished = true, fromVersion = null)
        dao.insertUpdate(device, release, "open", "1.2.0", null)

        dao.finishOpen(device, "cancelled", "replaced")

        assertEquals("failed", dao.findByRequestId("done")!!.status)
        val replaced = dao.findByRequestId("open")!!
        assertEquals("cancelled", replaced.status)
        assertEquals("replaced", replaced.detail)
        assertEquals(listOf("open", "done"), dao.listForDevice(device, 10).map { it.requestId })
    }

    @Test
    fun `WHEN a device reports its firmware twice THEN the last report is kept`() {
        val device = insertDevice()
        val first = DeviceFirmware("p", "1.1.0", "2026-11-07T19:50:09", "v5.5.3", 100, "aa", "ota_0", OffsetDateTime.now())
        dao.upsertFirmware(device, first)
        dao.upsertFirmware(device, first.copy(version = "1.2.0", slot = "ota_1"))

        val stored = dao.getFirmware(device)!!
        assertEquals("1.2.0", stored.version)
        assertEquals("ota_1", stored.slot)
        assertEquals(100L, stored.sizeBytes)
    }

    @Test
    fun `WHEN a release has an appended hash THEN the device's short hash finds it`() {
        val id = insertRelease("1.2.0")
        val hash = firmwareDao.getRelease(id)!!.imageHash!!

        assertEquals(id, firmwareDao.findReleaseByImageHash(hash.take(16))!!.id)
        assertEquals(id, firmwareDao.findReleaseByImageHash(hash.take(16).uppercase())!!.id)
        assertNull(firmwareDao.findReleaseByImageHash("0000000000000000"))
        assertEquals(id, firmwareDao.findRelease("croniot-watering-system", "1.2.0")!!.id)
    }
}
