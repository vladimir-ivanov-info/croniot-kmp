package com.server.croniot.services

import com.server.croniot.data.db.daos.IngestBatchDao
import com.server.croniot.data.db.daos.IngestBatchRow
import com.server.croniot.data.db.daos.StoredIngestBatch
import com.server.croniot.livelog.DeviceContext
import com.server.croniot.livelog.DeviceContextLookup
import com.server.croniot.livelog.LiveLogBus
import com.server.croniot.livelog.LogDeviceRef
import com.server.croniot.livelog.LogEntryDto
import com.server.croniot.livelog.LogLevels
import com.server.croniot.livelog.LogOwnerRef
import com.server.croniot.livelog.LogPlatform
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class IngestBatchRecorderTest {

    private val dao: IngestBatchDao = mockk()
    private val lookup: DeviceContextLookup = mockk()
    private val bus = LiveLogBus()
    private val recorder = IngestBatchRecorder(dao, bus, lookup)
    private val registry = SimpleMeterRegistry()

    private val context = DeviceContext(
        device = LogDeviceRef(uuid = "dev-uuid", name = "Riego"),
        owner = LogOwnerRef(uuid = "acc-uuid", nickname = "nick", email = "owner@example.com"),
    )

    @BeforeEach
    fun setUp() {
        Metrics.addRegistry(registry)
    }

    @AfterEach
    fun tearDown() {
        Metrics.removeRegistry(registry)
    }

    private fun row(duplicated: Int) = IngestBatchRow(
        deviceId = 7,
        stream = "logs",
        bootId = 95,
        firstSeq = 300,
        lastSeq = 331,
        records = 32,
        inserted = 32 - duplicated,
        duplicated = duplicated,
    )

    private fun stored(row: IngestBatchRow) = StoredIngestBatch(id = 12, row = row, receivedAt = OffsetDateTime.now())

    private fun duplicatesCount() = registry.find(IngestBatchRecorder.DUPLICATES_METRIC).tag("stream", "logs").counter()?.count() ?: 0.0

    @Test
    fun `WHEN a batch has no duplicates THEN it is stored but nothing else happens`() {
        val row = row(duplicated = 0)
        every { dao.insert(row) } returns stored(row)

        recorder.record(row)

        verify { dao.insert(row) }
        verify(exactly = 0) { lookup.byDeviceId(any()) }
        assertEquals(0.0, duplicatesCount())
    }

    @Test
    fun `WHEN a batch has duplicates THEN the counter grows by that many`() {
        val row = row(duplicated = 5)
        every { dao.insert(row) } returns stored(row)

        recorder.record(row)

        assertEquals(5.0, duplicatesCount())
    }

    @Test
    fun `WHEN a batch has duplicates and someone is watching THEN a Resent batch line is published`() = runBlocking {
        val row = row(duplicated = 32)
        every { dao.insert(row) } returns stored(row)
        every { lookup.byDeviceId(7) } returns context

        val received = async(start = CoroutineStart.UNDISPATCHED) { bus.messages.first() }
        withTimeout(5_000) { while (bus.subscriberCount < 1) yield() }
        recorder.record(row)

        val entry: LogEntryDto = withTimeout(5_000) { received.await() }.entry
        assertEquals("ing-12", entry.id)
        assertEquals(LogPlatform.SERVER, entry.platform)
        assertEquals(LogLevels.INFO, entry.level)
        assertEquals("Resent batch: 32/32 duplicate records (stream=logs, boot=95, seq 300-331)", entry.message)
        assertEquals(context.device, entry.device)
    }

    @Test
    fun `WHEN storing the row fails THEN record does not throw`() {
        val row = row(duplicated = 3)
        every { dao.insert(row) } throws IllegalStateException("db down")

        recorder.record(row)

        assertEquals(0.0, duplicatesCount())
    }
}
