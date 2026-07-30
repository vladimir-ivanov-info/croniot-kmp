package com.croniot.client.data.source.remote.ble

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BleWireFramesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `BleTaskFrame serializes and deserializes`() {
        val frame = BleTaskFrame(type = BleTaskFrameType.ADD_TASK, data = JsonPrimitive("payload"))

        val decoded = json.decodeFromString<BleTaskFrame>(json.encodeToString(frame))

        assertEquals(frame, decoded)
    }

    @Test
    fun `BleDeviceInfo serializes and deserializes`() {
        val info = BleDeviceInfo(deviceId = "device-1", protocolVersion = 1, schemaVersion = 42L)

        val decoded = json.decodeFromString<BleDeviceInfo>(json.encodeToString(info))

        assertEquals(info, decoded)
    }

    @Test
    fun `BleAuthRequest serializes and deserializes`() {
        val request = BleAuthRequest(username = "user", password = "pass")

        val decoded = json.decodeFromString<BleAuthRequest>(json.encodeToString(request))

        assertEquals(request, decoded)
    }

    @Test
    fun `BleAuthResponse serializes and deserializes with default error`() {
        val response = BleAuthResponse(ok = true)

        val decoded = json.decodeFromString<BleAuthResponse>(json.encodeToString(response))

        assertEquals(response, decoded)
        assertEquals(null, decoded.error)
    }

    @Test
    fun `BleAuthResponse carries an error message when present`() {
        val response = BleAuthResponse(ok = false, error = "bad credentials")

        val decoded = json.decodeFromString<BleAuthResponse>(json.encodeToString(response))

        assertEquals("bad credentials", decoded.error)
    }

    @Test
    fun `BleRequestSyncPayload serializes and deserializes`() {
        val payload = BleRequestSyncPayload(taskTypeUid = 7L)

        val decoded = json.decodeFromString<BleRequestSyncPayload>(json.encodeToString(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `BleSchemaDto serializes and deserializes with empty lists`() {
        val dto = BleSchemaDto(sensorTypes = emptyList(), taskTypes = emptyList())

        val decoded = json.decodeFromString<BleSchemaDto>(json.encodeToString(dto))

        assertEquals(dto, decoded)
    }

    @Test
    fun `BleSensorDataDto serializes and deserializes with default timestamp`() {
        val dto = BleSensorDataDto(sensorTypeUid = 1L, value = JsonPrimitive(23.5))

        val decoded = json.decodeFromString<BleSensorDataDto>(json.encodeToString(dto))

        assertEquals(dto, decoded)
        assertEquals(null, decoded.timestampMs)
    }

    @Test
    fun `BleSensorDataDto carries an explicit timestamp when present`() {
        val dto = BleSensorDataDto(sensorTypeUid = 1L, value = JsonPrimitive(23.5), timestampMs = 1000L)

        val decoded = json.decodeFromString<BleSensorDataDto>(json.encodeToString(dto))

        assertEquals(1000L, decoded.timestampMs)
    }

    @Test
    fun `BleTaskStateEventPayload serializes and deserializes with defaults`() {
        val payload = BleTaskStateEventPayload(taskTypeUid = 1L, state = "RUNNING")

        val decoded = json.decodeFromString<BleTaskStateEventPayload>(json.encodeToString(payload))

        assertEquals(payload, decoded)
        assertEquals(0L, decoded.taskUid)
        assertEquals(0.0, decoded.progress)
        assertEquals("", decoded.errorMessage)
    }

    @Test
    fun `BleTaskStateEventPayload carries explicit optional fields when present`() {
        val payload = BleTaskStateEventPayload(
            taskTypeUid = 1L,
            taskUid = 2L,
            state = "ERROR",
            progress = 0.5,
            errorMessage = "boom",
            timestampMs = 500L,
        )

        val decoded = json.decodeFromString<BleTaskStateEventPayload>(json.encodeToString(payload))

        assertEquals(payload, decoded)
    }
}
