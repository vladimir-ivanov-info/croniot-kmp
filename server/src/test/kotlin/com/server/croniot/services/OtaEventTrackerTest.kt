package com.server.croniot.services

import com.server.croniot.data.db.daos.OtaUpdate
import com.server.croniot.data.repositories.FirmwareRepository
import com.server.croniot.mqtt.DeviceLogRecord
import com.server.croniot.ota.OtaEvents
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class OtaEventTrackerTest {

    private val repo: FirmwareRepository = mockk(relaxUnitFun = true)
    private val tracker = OtaEventTracker(repo)

    private fun record(seq: Long, tag: String, message: String) =
        DeviceLogRecord(seq = seq, uptimeMs = seq, level = 2, tag = tag, message = message)

    private fun update(id: Long, requestId: String, deviceId: Long = 7, status: String = OtaEvents.REQUESTED) = OtaUpdate(
        id = id,
        deviceId = deviceId,
        releaseId = 12,
        requestId = requestId,
        version = "1.2.0",
        fromVersion = null,
        status = status,
        progress = 0,
        detail = null,
        requestedAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now(),
        finishedAt = null,
    )

    @Test
    fun `WHEN firmware_info arrives THEN the installed firmware is stored`() {
        tracker.onEvents(
            7,
            listOf(
                record(
                    1,
                    "firmware_info",
                    "project=croniot-watering-system version=v0.11.1-24-gd0e94ea built=2026-11-07T19:50:09 " +
                        "idf=v5.5.3 size=2010640 hash=670c0caa323fec74 slot=ota_0",
                ),
            ),
        )

        verify {
            repo.upsertFirmware(
                7,
                match {
                    it.version == "v0.11.1-24-gd0e94ea" && it.sizeBytes == 2010640L &&
                        it.imageHash == "670c0caa323fec74" && it.slot == "ota_0" && it.builtAt == "2026-11-07T19:50:09"
                },
            )
        }
    }

    @Test
    fun `WHEN an ota event names its request THEN that update moves`() {
        every { repo.findUpdate("r1") } returns update(3, "r1")

        tracker.onEvents(7, listOf(record(5, "ota_download_st", "version=1.2.0 from=1.1.0 requestId=r1")))

        verify { repo.updateState(3, OtaEvents.DOWNLOADING, 0, null, false, "1.1.0") }
    }

    @Test
    fun `WHEN the event has no request id or another device's THEN the newest open update is used`() {
        every { repo.findUpdate("other") } returns update(9, "other", deviceId = 99)
        every { repo.latestOpenUpdate(7) } returns update(4, "r2", status = OtaEvents.VALIDATING)

        tracker.onEvents(7, listOf(record(1, "ota_confirmed", "version=1.2.0 requestId=other")))

        verify { repo.updateState(4, OtaEvents.CONFIRMED, 100, null, true, null) }
        verify(exactly = 0) { repo.updateState(9, any(), any(), any(), any(), any()) }
    }

    @Test
    fun `WHEN a batch carries several events THEN they apply in device order and one failure skips only itself`() {
        val order = mutableListOf<String>()
        every { repo.findUpdate("r1") } returns update(3, "r1", status = OtaEvents.DOWNLOADING)
        every { repo.updateState(3, any(), any(), any(), any(), any()) } answers {
            order += secondArg<String>()
            if (order.size == 1) error("db down")
        }

        tracker.onEvents(
            7,
            listOf(
                record(11, "ota_installed", "version=1.2.0 requestId=r1"),
                record(10, "ota_progress", "version=1.2.0 percent=100 requestId=r1"),
            ),
        )

        assertEquals(listOf(OtaEvents.DOWNLOADING, OtaEvents.INSTALLED), order)
    }

    @Test
    fun `WHEN events are not about OTA THEN nothing is looked up`() {
        tracker.onEvents(7, listOf(record(1, "health", "m=heap")))
        verify(exactly = 0) { repo.latestOpenUpdate(any()) }
    }
}
