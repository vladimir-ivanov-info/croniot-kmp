package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceLogConfigRepository
import com.server.croniot.data.repositories.DeviceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceLogConfigServiceTest {

    private val deviceLogConfigRepository: DeviceLogConfigRepository = mockk(relaxUnitFun = true)
    private val deviceRepository: DeviceRepository = mockk()

    private val service = DeviceLogConfigService(
        deviceLogConfigRepository = deviceLogConfigRepository,
        deviceRepository = deviceRepository,
    )

    @Test
    fun `WHEN the device is unknown THEN setConfig fails and stores nothing`() {
        every { deviceRepository.getId("unknown-uuid") } returns null

        val result = service.setConfig("unknown-uuid", """{"default":"warn"}""")

        assertFalse(result.success)
        verify(exactly = 0) { deviceLogConfigRepository.upsert(any(), any()) }
    }

    @Test
    fun `WHEN the config is not a JSON object THEN setConfig fails and stores nothing`() {
        every { deviceRepository.getId("device-uuid") } returns 7L

        val result = service.setConfig("device-uuid", """["default","warn"]""")

        assertFalse(result.success)
        verify(exactly = 0) { deviceLogConfigRepository.upsert(any(), any()) }
    }

    @Test
    fun `WHEN the config is malformed JSON THEN setConfig fails and stores nothing`() {
        every { deviceRepository.getId("device-uuid") } returns 7L

        val result = service.setConfig("device-uuid", "not json")

        assertFalse(result.success)
        verify(exactly = 0) { deviceLogConfigRepository.upsert(any(), any()) }
    }

    @Test
    fun `WHEN the device is known and the config is a valid JSON object THEN setConfig stores it`() {
        every { deviceRepository.getId("device-uuid") } returns 7L

        val json = """{"default":"warn","tags":{"WifiMqttController":"trace"},"ttlSec":600}"""
        val result = service.setConfig("device-uuid", json)

        assertTrue(result.success)
        verify { deviceLogConfigRepository.upsert(7L, json) }
    }

    @Test
    fun `WHEN the device is unknown THEN getConfig returns null`() {
        every { deviceRepository.getId("unknown-uuid") } returns null
        assertNull(service.getConfig("unknown-uuid"))
    }

    @Test
    fun `WHEN the device has a stored config THEN getConfig returns it`() {
        every { deviceRepository.getId("device-uuid") } returns 7L
        every { deviceLogConfigRepository.get(7L) } returns """{"default":"info"}"""

        assertEquals("""{"default":"info"}""", service.getConfig("device-uuid"))
    }
}
