package com.server.croniot.livelog

import com.server.croniot.jooq.tables.ServerLog.Companion.SERVER_LOG
import com.server.croniot.testsupport.PostgresTestcontainer
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource

class ServerLogDbWriterIntegrationTest {

    private val dsl = PostgresTestcontainer.dsl
    private val queue = ServerLogQueue()
    private val writer = ServerLogDbWriter(dsl, queue, batchSize = 2, pollTimeoutMs = 10)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    private fun entry(id: String, level: Int = LogLevels.WARN) = LogEntryDto(
        id = id, platform = LogPlatform.SERVER, kind = LogKind.LOG, level = level,
        tag = "MqttDataProcessorDeviceLog", message = "msg $id", receivedAtMs = 1_760_000_000_000,
        thread = "worker-1", traceId = "trace-1", throwable = "java.lang.IllegalStateException: boom",
    )

    @Test
    fun `WHEN lines are queued THEN they are stored in batches with every field`() {
        listOf("srv-a-1", "srv-a-2", "srv-a-3").forEach { queue.offer("app", entry(it)) }

        assertEquals(2, writer.flushOnce())
        assertEquals(1, writer.flushOnce())
        assertEquals(0, writer.flushOnce())

        val rows = dsl.selectFrom(SERVER_LOG).orderBy(SERVER_LOG.ID).fetch()
        assertEquals(listOf("srv-a-1", "srv-a-2", "srv-a-3"), rows.map { it.entryId })
        val first = rows.first()
        assertEquals(LogLevels.WARN.toShort(), first.level)
        assertEquals("MqttDataProcessorDeviceLog", first.tag)
        assertEquals("msg srv-a-1", first.message)
        assertEquals("worker-1", first.thread)
        assertEquals("trace-1", first.traceId)
        assertEquals("java.lang.IllegalStateException: boom", first.throwable)
        assertEquals(1_760_000_000_000, first.createdAt!!.toInstant().toEpochMilli())
    }

    @Test
    fun `WHEN the same line is written twice THEN it is stored once`() {
        queue.offer("app", entry("srv-b-1"))
        writer.flushOnce()
        queue.offer("app", entry("srv-b-1"))
        writer.flushOnce()

        assertEquals(1, dsl.fetchCount(SERVER_LOG))
    }

    @Test
    fun `WHEN the database is unreachable THEN the batch is dropped and counted without throwing`() {
        val deadDsl = DSL.using(
            PGSimpleDataSource().apply { setURL("jdbc:postgresql://127.0.0.1:1/nothing?connectTimeout=1") },
            SQLDialect.POSTGRES,
        )
        val deadWriter = ServerLogDbWriter(deadDsl, queue, batchSize = 10, pollTimeoutMs = 10)
        queue.offer("app", entry("srv-c-1"))
        queue.offer("app", entry("srv-c-2"))

        assertEquals(2, deadWriter.flushOnce())
        assertEquals(2L, deadWriter.failedLines)
    }

    @Test
    fun `WHEN stopped THEN whatever is still queued is written`() {
        writer.start()
        writer.stop()
        (1..5).forEach { queue.offer("app", entry("srv-d-$it")) }

        writer.stop()

        assertEquals(5, dsl.fetchCount(SERVER_LOG))
    }
}
