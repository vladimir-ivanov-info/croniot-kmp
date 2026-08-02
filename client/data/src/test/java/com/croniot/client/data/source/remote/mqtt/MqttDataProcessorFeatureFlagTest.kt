package com.croniot.client.data.source.remote.mqtt

import android.util.Log
import croniot.messages.MessageFactory
import croniot.models.dto.FeatureFlagDto
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MqttDataProcessorFeatureFlagTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `WHEN processing a flag topic THEN process extracts the flag name and overrides the dto name`() {
        val received = mutableListOf<FeatureFlagDto>()
        val processor = MqttDataProcessorFeatureFlag(onFlagUpdate = { received.add(it) })
        val dto = FeatureFlagDto(name = "irrelevant", enabled = true)
        val json = MessageFactory.toJson(dto)

        processor.process("/server/feature_flags/dark_mode", json)

        assertEquals(1, received.size)
        assertEquals("dark_mode", received.first().name)
        assertTrue(received.first().enabled)
    }

    @Test
    fun `WHEN topic is missing the flag name segment THEN process does not invoke callback`() {
        val received = mutableListOf<FeatureFlagDto>()
        val processor = MqttDataProcessorFeatureFlag(onFlagUpdate = { received.add(it) })
        val json = MessageFactory.toJson(FeatureFlagDto(name = "x", enabled = true))

        processor.process("/server/feature_flags", json)

        assertEquals(0, received.size)
    }

    @Test
    fun `WHEN topic has the wrong prefix THEN process does not invoke callback`() {
        val received = mutableListOf<FeatureFlagDto>()
        val processor = MqttDataProcessorFeatureFlag(onFlagUpdate = { received.add(it) })
        val json = MessageFactory.toJson(FeatureFlagDto(name = "x", enabled = true))

        processor.process("/other/feature_flags/dark_mode", json)

        assertEquals(0, received.size)
    }

    @Test
    fun `WHEN json is invalid THEN process does not invoke callback`() {
        val received = mutableListOf<FeatureFlagDto>()
        val processor = MqttDataProcessorFeatureFlag(onFlagUpdate = { received.add(it) })

        processor.process("/server/feature_flags/dark_mode", "not valid json {")

        assertEquals(0, received.size)
    }

    @Test
    fun `WHEN the flag is disabled THEN process preserves the enabled false value`() {
        val received = mutableListOf<FeatureFlagDto>()
        val processor = MqttDataProcessorFeatureFlag(onFlagUpdate = { received.add(it) })
        val json = MessageFactory.toJson(FeatureFlagDto(name = "x", enabled = false))

        processor.process("/server/feature_flags/beta_feature", json)

        assertEquals(false, received.first().enabled)
    }
}
