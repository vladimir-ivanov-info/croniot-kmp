package com.server.croniot.livelog

import com.server.croniot.data.db.daos.InsertedDeviceLogRow
import com.server.croniot.mqtt.DeviceLogRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DeviceLogLivePublisherTest {

    private val bus = LiveLogBus()
    private val lookup: DeviceContextLookup = mockk()
    private val publisher = DeviceLogLivePublisher(bus, lookup)

    private val context = DeviceContext(
        device = LogDeviceRef(uuid = "dev-uuid", name = "Riego"),
        owner = LogOwnerRef(uuid = "acc-uuid", nickname = "nick", email = "owner@example.com"),
    )

    private val receivedAt = OffsetDateTime.of(2026, 10, 11, 18, 0, 0, 0, ZoneOffset.UTC)

    private fun row(id: Long, seq: Long, level: Int = LogLevels.WARN) = InsertedDeviceLogRow(
        id = id,
        record = DeviceLogRecord(seq = seq, uptimeMs = seq * 100, level = level, tag = "Uplink", message = "msg $seq"),
        receivedAt = receivedAt,
    )

    private fun collect(count: Int, block: () -> Unit): List<LogEntryDto> = runBlocking {
        val received = async(start = CoroutineStart.UNDISPATCHED) { bus.messages.take(count).toList() }
        withTimeout(5_000) { while (bus.subscriberCount < 1) yield() }
        block()
        withTimeout(5_000) { received.await() }.map { it.entry }
    }

    @Test
    fun `WHEN nobody is watching THEN it neither looks up the device nor publishes`() {
        publisher.publish(deviceId = 7, bootId = 3, kind = LogKind.LOG, rows = listOf(row(1, 10)))

        verify(exactly = 0) { lookup.byDeviceId(any()) }
    }

    @Test
    fun `WHEN rows are published THEN each becomes an IOT entry with device, owner and device fields`() {
        every { lookup.byDeviceId(7) } returns context

        val entries = collect(2) {
            publisher.publish(deviceId = 7, bootId = 3, kind = LogKind.LOG, rows = listOf(row(1, 10), row(2, 11)))
        }

        assertEquals(listOf("log-1", "log-2"), entries.map { it.id })
        val first = entries.first()
        assertEquals(LogPlatform.IOT, first.platform)
        assertEquals(LogKind.LOG, first.kind)
        assertEquals(LogLevels.WARN, first.level)
        assertEquals("Uplink", first.tag)
        assertEquals("msg 10", first.message)
        assertEquals(receivedAt.toInstant().toEpochMilli(), first.receivedAtMs)
        assertEquals(context.device, first.device)
        assertEquals(context.owner, first.owner)
        assertEquals(3L, first.bootId)
        assertEquals(10L, first.seq)
        assertEquals(1000L, first.uptimeMs)
        verify(exactly = 1) { lookup.byDeviceId(7) }
    }

    @Test
    fun `WHEN the kind is EVENT THEN ids use the evt prefix`() {
        every { lookup.byDeviceId(7) } returns context

        val entries = collect(1) {
            publisher.publish(deviceId = 7, bootId = 3, kind = LogKind.EVENT, rows = listOf(row(5, 10)))
        }

        assertEquals("evt-5", entries.single().id)
        assertEquals(LogKind.EVENT, entries.single().kind)
    }

    @Test
    fun `WHEN the device cannot be resolved THEN the entry is still published without device or owner`() {
        every { lookup.byDeviceId(7) } returns null

        val entry = collect(1) {
            publisher.publish(deviceId = 7, bootId = 3, kind = LogKind.LOG, rows = listOf(row(1, 10)))
        }.single()

        assertNull(entry.device)
        assertNull(entry.owner)
    }
}
