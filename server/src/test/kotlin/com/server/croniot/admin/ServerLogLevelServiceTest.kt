package com.server.croniot.admin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ServerLogLevelServiceTest {

    private val context = LoggerContext().apply {
        getLogger(Logger.ROOT_LOGGER_NAME).level = Level.INFO
    }
    private val scope = TestScope()
    private val service = ServerLogLevelService(context, scope) { 1_000L }

    private fun level(name: String) = context.getLogger(name).effectiveLevel

    @Test
    fun `WHEN the root level is raised THEN it applies until the TTL, then reverts`() {
        val dto = service.set(SetServerLogLevelRequest(level = "debug", ttlMinutes = 10))

        assertEquals("DEBUG", dto.root)
        assertEquals(1_000L + 10 * 60_000, dto.revertsAtMs)
        assertEquals(Level.DEBUG, level("com.server.croniot.mqtt.Anything"))

        scope.advanceTimeBy(10 * 60_000L + 1)
        scope.runCurrent()

        assertEquals(Level.INFO, level(Logger.ROOT_LOGGER_NAME))
        assertNull(service.current().revertsAtMs)
    }

    @Test
    fun `WHEN one logger is changed THEN only it and its children change`() {
        service.set(SetServerLogLevelRequest(level = "TRACE", logger = "com.server.croniot.mqtt"))

        assertEquals(Level.TRACE, level("com.server.croniot.mqtt.MqttController"))
        assertEquals(Level.INFO, level("io.ktor.server.Application"))
        assertEquals(mapOf("com.server.croniot.mqtt" to "TRACE"), service.current().overrides)
    }

    @Test
    fun `WHEN reverted by hand THEN every changed logger gets its original level back`() {
        service.set(SetServerLogLevelRequest(level = "DEBUG"))
        service.set(SetServerLogLevelRequest(level = "TRACE", logger = "com.server.croniot.mqtt"))

        val dto = service.revert()

        assertEquals("INFO", dto.root)
        assertEquals(Level.INFO, level("com.server.croniot.mqtt.MqttController"))
        assertTrue(dto.overrides.isEmpty())
    }

    @Test
    fun `WHEN changed twice THEN the original level is still the one restored`() {
        service.set(SetServerLogLevelRequest(level = "DEBUG"))
        service.set(SetServerLogLevelRequest(level = "TRACE"))

        service.revert()

        assertEquals(Level.INFO, level(Logger.ROOT_LOGGER_NAME))
    }

    @Test
    fun `WHEN the level is unknown THEN it is rejected and nothing changes`() {
        assertThrows<IllegalArgumentException> { service.set(SetServerLogLevelRequest(level = "LOUD")) }

        assertEquals(Level.INFO, level(Logger.ROOT_LOGGER_NAME))
    }
}
