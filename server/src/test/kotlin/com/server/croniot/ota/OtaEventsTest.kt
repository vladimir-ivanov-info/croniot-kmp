package com.server.croniot.ota

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OtaEventsTest {

    private fun t(tag: String, message: String, status: String = OtaEvents.REQUESTED, progress: Int = 0) =
        OtaEvents.transition(tag, OtaEvents.parseFields(message), status, progress)

    @Test
    fun `WHEN a message has values with spaces THEN each value runs to the next key`() {
        val fields = OtaEvents.parseFields("version=1.2.0 requestId=r1 reason=download: http 404")
        assertEquals("1.2.0", fields["version"])
        assertEquals("r1", fields["requestId"])
        assertEquals("download: http 404", fields["reason"])
    }

    @Test
    fun `WHEN a message is empty THEN there are no fields`() {
        assertTrue(OtaEvents.parseFields("").isEmpty())
    }

    @Test
    fun `WHEN an update runs normally THEN it goes download, progress, installed, validating, confirmed`() {
        val started = t("ota_download_started", "version=1.2.0 from=1.1.0 requestId=r1")!!
        assertEquals(OtaEvents.DOWNLOADING, started.status)
        assertEquals("1.1.0", started.fromVersion)

        assertEquals(40, t("ota_progress", "version=1.2.0 percent=40 requestId=r1", OtaEvents.DOWNLOADING, 30)!!.progress)
        assertEquals(OtaEvents.INSTALLED, t("ota_installed", "version=1.2.0", OtaEvents.DOWNLOADING, 100)!!.status)
        assertEquals(OtaEvents.VALIDATING, t("ota_validating", "version=1.2.0", OtaEvents.INSTALLED)!!.status)

        val confirmed = t("ota_confirmed", "version=1.2.0", OtaEvents.VALIDATING)!!
        assertEquals(OtaEvents.CONFIRMED, confirmed.status)
        assertTrue(confirmed.finished)
    }

    @Test
    fun `WHEN progress arrives out of order or after the download THEN it never goes back`() {
        assertEquals(60, t("ota_progress", "percent=50", OtaEvents.DOWNLOADING, 60)!!.progress)
        assertNull(t("ota_progress", "percent=90", OtaEvents.INSTALLED, 100))
        assertNull(t("ota_progress", "percent=abc", OtaEvents.DOWNLOADING, 10))
    }

    @Test
    fun `WHEN an update already finished THEN later events change nothing`() {
        assertNull(t("ota_progress", "percent=50", OtaEvents.CANCELLED))
        assertNull(t("ota_confirmed", "version=1.2.0", OtaEvents.FAILED))
    }

    @Test
    fun `WHEN a download breaks THEN the update waits for a retry, but a bad image is final`() {
        val broken = t("ota_failed", "version=1.2.0 reason=download: read", OtaEvents.DOWNLOADING, 30)!!
        assertEquals(OtaEvents.RETRYING, broken.status)
        assertFalse(broken.finished)
        assertEquals(30, broken.progress)

        val wrong = t("ota_failed", "version=1.2.0 reason=sha256", OtaEvents.DOWNLOADING, 100)!!
        assertEquals(OtaEvents.FAILED, wrong.status)
        assertTrue(wrong.finished)
        assertTrue(wrong.detail!!.contains("does not match"))
    }

    @Test
    fun `WHEN validation fails THEN it is rolling back until the old firmware reports the rollback`() {
        val failing = t("ota_validation_failed", "version=1.2.0 reason=no_connectivity", OtaEvents.VALIDATING)!!
        assertEquals(OtaEvents.ROLLING_BACK, failing.status)
        assertFalse(failing.finished)

        val final = t("ota_rolled_back", "version=1.2.0 running=1.1.0 reason=boot bad=true", OtaEvents.ROLLING_BACK)!!
        assertEquals(OtaEvents.ROLLED_BACK, final.status)
        assertTrue(final.finished)

        val again = t("ota_rolled_back", "version=1.2.0 running=1.1.0 reason=boot bad=false", OtaEvents.INSTALLED)!!
        assertEquals(OtaEvents.RETRYING, again.status)
        assertFalse(again.finished)
    }

    @Test
    fun `WHEN the device ignores the request THEN the update is finished with the reason`() {
        val ignored = t("ota_request_ignored", "version=1.2.0 requestId=r1 reason=bad_release")!!
        assertEquals(OtaEvents.IGNORED, ignored.status)
        assertTrue(ignored.finished)
        assertTrue(ignored.detail!!.contains("failed before"))
    }

    @Test
    fun `WHEN an event is not about the update THEN it changes nothing`() {
        assertNull(t("ota_request_invalid", ""))
        assertNull(t("health", "m=heap"))
    }

    @Test
    fun `WHEN the device cut the tag to 15 characters THEN the event is still recognised`() {
        // As stored by the server on 2026-11-07 from a real ESP32-C6.
        assertEquals("ota_download_started", OtaEvents.eventName("ota_download_st"))
        assertEquals("ota_validation_failed", OtaEvents.eventName("ota_validation_"))
        assertEquals("ota_self_test_failed", OtaEvents.eventName("ota_self_test_f"))
        assertEquals("ota_request_ignored", OtaEvents.eventName("ota_request_ign"))
        assertEquals("ota_rolled_back", OtaEvents.eventName("ota_rolled_back"))
        assertNull(OtaEvents.eventName("ota_unknown"))

        assertEquals(OtaEvents.DOWNLOADING, t("ota_download_st", "version=b from=a requestId=r1")!!.status)
        assertEquals(OtaEvents.ROLLING_BACK, t("ota_validation_", "reason=self_test", OtaEvents.VALIDATING)!!.status)
        assertEquals(OtaEvents.IGNORED, t("ota_request_ign", "reason=same_version")!!.status)
    }

    @Test
    fun `WHEN the events of a real update are replayed THEN it ends confirmed`() {
        // device_event rows from the ESP32-C6 test of 2026-11-07, in order.
        val events = listOf(
            "ota_download_st" to "version=ota-progress-test-b from=v0.11.1-24-gd0e94ea requestId=t-progress-1",
        ) + (0..100 step 10).map {
            "ota_progress" to "version=ota-progress-test-b percent=$it requestId=t-progress-1"
        } + listOf(
            "ota_installed" to "version=ota-progress-test-b from=v0.11.1-24-gd0e94ea attempt=1 requestId=t-progress-1",
            "ota_validating" to "version=ota-progress-test-b from=v0.11.1-24-gd0e94ea attempt=1 requestId=t-progress-1",
            "ota_confirmed" to "version=ota-progress-test-b from=v0.11.1-24-gd0e94ea attempt=1 requestId=t-progress-1",
        )
        var status = OtaEvents.REQUESTED
        var progress = 0
        var finished = false
        val seen = mutableListOf<Int>()
        for ((tag, message) in events) {
            val step = t(tag, message, status, progress) ?: continue
            status = step.status
            progress = step.progress ?: progress
            finished = step.finished
            if (status == OtaEvents.DOWNLOADING) seen += progress
        }
        assertEquals(OtaEvents.CONFIRMED, status)
        assertTrue(finished)
        assertEquals(listOf(0, 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100), seen)
    }
}
