package com.server.croniot.livelog

import com.server.croniot.controllers.DroppedNotice
import com.server.croniot.controllers.LiveLogGapTracker
import io.ktor.http.parametersOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LiveLogFilterTest {

    private fun entry(
        platform: LogPlatform = LogPlatform.IOT,
        kind: LogKind = LogKind.LOG,
        level: Int = LogLevels.INFO,
        device: String? = "dev-1",
        owner: String? = "acc-1",
        receivedAtMs: Long = 0,
    ) = LogEntryDto(
        id = "x", platform = platform, kind = kind, level = level, tag = "T", message = "m",
        receivedAtMs = receivedAtMs,
        device = device?.let { LogDeviceRef(it, null) },
        owner = owner?.let { LogOwnerRef(it, null, "e") },
    )

    @Test
    fun `WHEN there are no parameters THEN everything matches`() {
        val filter = LiveLogFilter.from(parametersOf())

        assertTrue(filter.matches(entry()))
        assertTrue(filter.matches(entry(platform = LogPlatform.SERVER, device = null, owner = null, level = LogLevels.TRACE)))
    }

    @Test
    fun `WHEN several values are given THEN any of them matches, repeated or comma separated`() {
        val filter = LiveLogFilter.from(
            parametersOf("platform" to listOf("server", "IOT"), "level" to listOf("error,warn")),
        )

        assertEquals(setOf(LogPlatform.SERVER, LogPlatform.IOT), filter.platforms)
        assertEquals(setOf(LogLevels.ERROR, LogLevels.WARN), filter.levels)
        assertTrue(filter.matches(entry(level = LogLevels.ERROR)))
        assertTrue(filter.matches(entry(platform = LogPlatform.SERVER, level = LogLevels.WARN)))
        assertFalse(filter.matches(entry(level = LogLevels.INFO)))
        assertFalse(filter.matches(entry(platform = LogPlatform.ANDROID, level = LogLevels.ERROR)))
    }

    @Test
    fun `WHEN filtering by device or owner THEN server lines without them do not match`() {
        val byDevice = LiveLogFilter.from(parametersOf("device", "dev-1"))
        val byOwner = LiveLogFilter.from(parametersOf("owner", "acc-2"))

        assertTrue(byDevice.matches(entry()))
        assertFalse(byDevice.matches(entry(platform = LogPlatform.SERVER, device = null, owner = null)))
        assertFalse(byOwner.matches(entry()))
        assertTrue(byOwner.matches(entry(owner = "acc-2")))
    }

    @Test
    fun `WHEN filtering by kind and numeric level THEN both apply`() {
        val filter = LiveLogFilter.from(parametersOf("kind" to listOf("event"), "level" to listOf("0", "9")))

        assertEquals(setOf(LogLevels.ERROR), filter.levels)
        assertTrue(filter.matches(entry(kind = LogKind.EVENT, level = LogLevels.ERROR)))
        assertFalse(filter.matches(entry(kind = LogKind.LOG, level = LogLevels.ERROR)))
    }

    @Test
    fun `WHEN unknown values are given THEN they are ignored`() {
        val filter = LiveLogFilter.from(parametersOf("platform" to listOf("toaster"), "level" to listOf("loud")))

        assertTrue(filter.platforms.isEmpty())
        assertTrue(filter.levels.isEmpty())
    }

    @Test
    fun `WHEN busSeq is contiguous THEN the gap tracker reports nothing`() {
        val tracker = LiveLogGapTracker()

        assertNull(tracker.onMessage(LiveLogMessage(1, entry(receivedAtMs = 10))))
        assertNull(tracker.onMessage(LiveLogMessage(2, entry(receivedAtMs = 20))))
    }

    @Test
    fun `WHEN busSeq jumps THEN the gap tracker reports the missing count and time range`() {
        val tracker = LiveLogGapTracker()
        tracker.onMessage(LiveLogMessage(4, entry(receivedAtMs = 100)))

        val notice = tracker.onMessage(LiveLogMessage(9, entry(receivedAtMs = 900)))

        assertEquals(DroppedNotice(count = 4, fromMs = 100, toMs = 900), notice)
    }
}
