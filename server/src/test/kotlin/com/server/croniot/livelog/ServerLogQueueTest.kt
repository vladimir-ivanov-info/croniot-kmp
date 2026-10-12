package com.server.croniot.livelog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerLogQueueTest {

    private fun entry(n: Int) = LogEntryDto(
        id = "srv-$n", platform = LogPlatform.SERVER, kind = LogKind.LOG, level = LogLevels.INFO,
        tag = "T", message = "m$n", receivedAtMs = n.toLong(),
    )

    @Test
    fun `WHEN lines come from the database stack or the writer itself THEN they are not queued`() {
        assertTrue(ServerLogQueue.isExcluded("org.jooq.tools.LoggerListener"))
        assertTrue(ServerLogQueue.isExcluded("com.zaxxer.hikari.pool.HikariPool"))
        assertTrue(ServerLogQueue.isExcluded("org.postgresql.Driver"))
        assertTrue(ServerLogQueue.isExcluded("com.server.croniot.livelog.ServerLogDbWriter"))
        assertFalse(ServerLogQueue.isExcluded("com.server.croniot.mqtt.MqttDataProcessorDeviceLog"))
        assertFalse(ServerLogQueue.isExcluded("io.ktor.server.Application"))

        val queue = ServerLogQueue()
        queue.offer("org.jooq.Foo", entry(1))
        assertEquals(emptyList<LogEntryDto>(), queue.drain(max = 10, timeoutMs = 10))
    }

    @Test
    fun `WHEN the queue is full THEN new lines are dropped and counted, never blocking`() {
        val queue = ServerLogQueue(capacity = 2)

        (1..5).forEach { queue.offer("app", entry(it)) }

        assertEquals(3L, queue.droppedCount)
        assertEquals(listOf("srv-1", "srv-2"), queue.drain(max = 10, timeoutMs = 10).map { it.id })
    }

    @Test
    fun `WHEN draining THEN at most max lines are taken, in order`() {
        val queue = ServerLogQueue()
        (1..5).forEach { queue.offer("app", entry(it)) }

        assertEquals(listOf("srv-1", "srv-2", "srv-3"), queue.drain(max = 3, timeoutMs = 10).map { it.id })
        assertEquals(listOf("srv-4", "srv-5"), queue.drain(max = 3, timeoutMs = 10).map { it.id })
    }
}
