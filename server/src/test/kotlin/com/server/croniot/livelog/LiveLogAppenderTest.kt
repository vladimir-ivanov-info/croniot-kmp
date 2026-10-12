package com.server.croniot.livelog

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import com.server.croniot.application.MDC_TRACE_ID
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class LiveLogAppenderTest {

    private val bus = LiveLogBus()

    // A private LoggerContext, so this test neither depends on nor
    // disturbs the real logback.xml configuration.
    private val context = LoggerContext().apply {
        // A hand-made context has no MDC adapter; share the one MDC.put() uses.
        setMDCAdapter(MDC.getMDCAdapter())
    }
    private val appender = LiveLogAppender(bus).apply {
        context = this@LiveLogAppenderTest.context
        start()
    }
    private val logger: Logger = context.getLogger("com.server.croniot.mqtt.MqttDataProcessorDeviceLog").apply {
        level = Level.TRACE
        isAdditive = false
        addAppender(appender)
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
        context.stop()
    }

    private fun collect(count: Int, block: () -> Unit): List<LogEntryDto> = runBlocking {
        val received = async(start = CoroutineStart.UNDISPATCHED) { bus.messages.take(count).toList() }
        withTimeout(5_000) { while (bus.subscriberCount < 1) yield() }
        block()
        withTimeout(5_000) { received.await() }.map { it.entry }
    }

    @Test
    fun `WHEN a line is logged THEN it becomes a SERVER entry with tag, thread and trace id`() {
        MDC.put(MDC_TRACE_ID, "trace-1")

        var loggingThread = ""
        val entry = collect(1) {
            loggingThread = Thread.currentThread().name
            logger.warn("unknown device {}", "dev-9")
        }.single()

        assertEquals(LogPlatform.SERVER, entry.platform)
        assertEquals(LogKind.LOG, entry.kind)
        assertEquals(LogLevels.WARN, entry.level)
        assertEquals("MqttDataProcessorDeviceLog", entry.tag)
        assertEquals("unknown device dev-9", entry.message)
        assertEquals(loggingThread, entry.thread)
        assertEquals("trace-1", entry.traceId)
        assertNull(entry.throwable)
        assertTrue(entry.id.startsWith("srv-"))
    }

    @Test
    fun `WHEN levels differ THEN they map onto the shared 0 to 4 scale`() {
        val entries = collect(5) {
            logger.error("e")
            logger.warn("w")
            logger.info("i")
            logger.debug("d")
            logger.trace("t")
        }

        assertEquals(
            listOf(LogLevels.ERROR, LogLevels.WARN, LogLevels.INFO, LogLevels.DEBUG, LogLevels.TRACE),
            entries.map { it.level },
        )
    }

    @Test
    fun `WHEN an exception is logged THEN its class and message are kept`() {
        val entry = collect(1) { logger.error("boom", IllegalStateException("db down")) }.single()

        assertEquals("java.lang.IllegalStateException: db down", entry.throwable)
    }

    @Test
    fun `WHEN the message has ANSI colour codes THEN they are stripped`() {
        val entry = collect(1) { logger.info("\u001B[32m200 OK\u001B[m: \u001B[36mPOST\u001B[m - /hour") }.single()

        assertEquals("200 OK: POST - /hour", entry.message)
    }

    @Test
    fun `WHEN two lines are logged THEN they get different ids`() {
        val entries = collect(2) {
            logger.info("a")
            logger.info("b")
        }

        assertNotEquals(entries[0].id, entries[1].id)
    }
}
