package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.SensorTypeRepository
import croniot.messages.MessageRegisterSensorType
import croniot.models.SensorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SensorTypeServiceTest {

    private val sensorTypeRepository: SensorTypeRepository = mockk(relaxUnitFun = true)
    private val deviceTokenRepository: DeviceTokenRepository = mockk()
    private val deviceRepository: DeviceRepository = mockk()

    private val service = SensorTypeService(
        sensorTypeRepository = sensorTypeRepository,
        deviceTokenRepository = deviceTokenRepository,
        deviceRepository = deviceRepository,
    )

    private val sensorType = SensorType(uid = 7L, name = "Temperature", description = "d", parameters = emptyList())

    private val message = MessageRegisterSensorType(
        deviceUuid = "device-uuid",
        deviceToken = "device-token",
        sensorType = sensorType,
    )

    @Test
    fun `WHEN device does not exist THEN registerSensorType returns failure`() {
        every { deviceRepository.isDeviceExists("device-uuid") } returns false
        every { deviceTokenRepository.isTokenCorrect("device-uuid", "device-token") } returns true

        val result = service.registerSensorType(message)

        assertFalse(result.success)
        assertEquals("Incorrect device or token for sensor register process.", result.message)
        verify(exactly = 0) { sensorTypeRepository.upsert(any(), any()) }
    }

    @Test
    fun `WHEN token is incorrect THEN registerSensorType returns failure`() {
        every { deviceRepository.isDeviceExists("device-uuid") } returns true
        every { deviceTokenRepository.isTokenCorrect("device-uuid", "device-token") } returns false

        val result = service.registerSensorType(message)

        assertFalse(result.success)
        assertEquals("Incorrect device or token for sensor register process.", result.message)
        verify(exactly = 0) { sensorTypeRepository.upsert(any(), any()) }
    }

    @Test
    fun `WHEN device id cannot be resolved THEN registerSensorType returns failure`() {
        every { deviceRepository.isDeviceExists("device-uuid") } returns true
        every { deviceTokenRepository.isTokenCorrect("device-uuid", "device-token") } returns true
        every { deviceRepository.getId("device-uuid") } returns null

        val result = service.registerSensorType(message)

        assertFalse(result.success)
        assertEquals("Incorrect device or token for sensor register process.", result.message)
        verify(exactly = 0) { sensorTypeRepository.upsert(any(), any()) }
    }

    @Test
    fun `WHEN inputs are valid THEN registerSensorType upserts the sensor type and returns success`() {
        every { deviceRepository.isDeviceExists("device-uuid") } returns true
        every { deviceTokenRepository.isTokenCorrect("device-uuid", "device-token") } returns true
        every { deviceRepository.getId("device-uuid") } returns 5L

        val result = service.registerSensorType(message)

        assertTrue(result.success)
        assertEquals("Sensor 7 registered", result.message)
        verify(exactly = 1) { sensorTypeRepository.upsert(sensorType, 5L) }
    }
}
