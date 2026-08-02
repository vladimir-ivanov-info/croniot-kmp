package com.server.croniot.http

import com.server.croniot.application.ApplicationScope
import com.server.croniot.mqtt.MqttController
import com.server.croniot.services.DeviceService
import croniot.messages.MessageSensorData
import croniot.models.Device
import croniot.models.dto.SensorDataDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SensorsDataControllerTest {

    private val deviceService: DeviceService = mockk()
    private val applicationScope = ApplicationScope()
    private val controller = SensorsDataController(deviceService, applicationScope)

    @BeforeEach
    fun setUp() {
        mockkObject(MqttController)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(MqttController)
        applicationScope.shutdown()
    }

    @Test
    fun `WHEN the device exists THEN processSensorData forwards to MqttController`() {
        val device = Device(uuid = "device-uuid", name = "d", iot = true)
        every { deviceService.getLazy("device-uuid") } returns device
        val captured = slot<SensorDataDto>()
        coEvery { MqttController.sendSensorData(capture(captured)) } returns Unit

        controller.processSensorData("device-uuid", MessageSensorData(sensorTypeId = 7L, value = "23.5"))

        coVerify(timeout = 2000, exactly = 1) { MqttController.sendSensorData(any()) }
        assertEquals("device-uuid", captured.captured.deviceUuid)
        assertEquals(7L, captured.captured.sensorTypeUid)
        assertEquals("23.5", captured.captured.value)
    }

    @Test
    fun `WHEN the device does not exist THEN processSensorData does not forward to MqttController`() {
        every { deviceService.getLazy("unknown-device") } returns null

        controller.processSensorData("unknown-device", MessageSensorData(sensorTypeId = 7L, value = "23.5"))

        coVerify(exactly = 0) { MqttController.sendSensorData(any()) }
    }
}
