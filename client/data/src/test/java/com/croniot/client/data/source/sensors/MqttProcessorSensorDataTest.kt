package com.croniot.client.data.source.sensors

import android.util.Log
import croniot.messages.MessageFactory
import croniot.models.dto.SensorDataDto
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class MqttProcessorSensorDataTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `process deserializes valid sensor data and invokes callback`() {
        val received = mutableListOf<SensorDataDto>()
        val processor = MqttProcessorSensorData(onNewSensorDataDto = { received.add(it) })
        val dto = SensorDataDto(deviceUuid = "device-1", sensorTypeUid = 10L, value = "25.5", timestamp = ZonedDateTime.now())
        val json = MessageFactory.toJson(dto)

        processor.process("some/topic", json)

        assertEquals(1, received.size)
        assertEquals("device-1", received.first().deviceUuid)
        assertEquals("25.5", received.first().value)
    }

    @Test
    fun `process with invalid json does not invoke callback`() {
        val received = mutableListOf<SensorDataDto>()
        val processor = MqttProcessorSensorData(onNewSensorDataDto = { received.add(it) })

        processor.process("some/topic", "not valid json {")

        assertEquals(0, received.size)
    }

    @Test
    fun `process with non-string data does not invoke callback`() {
        val received = mutableListOf<SensorDataDto>()
        val processor = MqttProcessorSensorData(onNewSensorDataDto = { received.add(it) })

        processor.process("some/topic", 42)

        assertEquals(0, received.size)
    }
}
