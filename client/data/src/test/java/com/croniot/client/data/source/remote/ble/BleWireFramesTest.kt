package com.croniot.client.data.source.remote.ble

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BleWireFramesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `WHEN BleTaskFrame is serialized THEN it deserializes back to the original`() {
        val frame = BleTaskFrame(type = BleTaskFrameType.ADD_TASK, data = JsonPrimitive("payload"))

        val decoded = json.decodeFromString<BleTaskFrame>(json.encodeToString(frame))

        assertEquals(frame, decoded)
    }

    @Test
    fun `WHEN BleDeviceInfo is serialized THEN it deserializes back to the original`() {
        val info = BleDeviceInfo(deviceId = "device-1", protocolVersion = 1, schemaVersion = 42L)

        val decoded = json.decodeFromString<BleDeviceInfo>(json.encodeToString(info))

        assertEquals(info, decoded)
    }

    @Test
    fun `WHEN BleAuthRequest is serialized THEN it deserializes back to the original`() {
        val request = BleAuthRequest(username = "user", password = "pass")

        val decoded = json.decodeFromString<BleAuthRequest>(json.encodeToString(request))

        assertEquals(request, decoded)
    }

    @Test
    fun `WHEN BleAuthResponse is serialized without an error THEN it deserializes with the default null error`() {
        val response = BleAuthResponse(ok = true)

        val decoded = json.decodeFromString<BleAuthResponse>(json.encodeToString(response))

        assertEquals(response, decoded)
        assertEquals(null, decoded.error)
    }

    @Test
    fun `WHEN BleAuthResponse has an error message THEN it carries it through serialization`() {
        val response = BleAuthResponse(ok = false, error = "bad credentials")

        val decoded = json.decodeFromString<BleAuthResponse>(json.encodeToString(response))

        assertEquals("bad credentials", decoded.error)
    }

    @Test
    fun `WHEN BleRequestSyncPayload is serialized THEN it deserializes back to the original`() {
        val payload = BleRequestSyncPayload(taskTypeUid = 7L)

        val decoded = json.decodeFromString<BleRequestSyncPayload>(json.encodeToString(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `WHEN BleSchemaDto has empty lists THEN it serializes and deserializes correctly`() {
        val dto = BleSchemaDto(sensorTypes = emptyList(), taskTypes = emptyList())

        val decoded = json.decodeFromString<BleSchemaDto>(json.encodeToString(dto))

        assertEquals(dto, decoded)
    }

    @Test
    fun `WHEN BleSensorDataDto is serialized without a timestamp THEN it deserializes with the default null timestamp`() {
        val dto = BleSensorDataDto(sensorTypeUid = 1L, value = JsonPrimitive(23.5))

        val decoded = json.decodeFromString<BleSensorDataDto>(json.encodeToString(dto))

        assertEquals(dto, decoded)
        assertEquals(null, decoded.timestampMs)
    }

    @Test
    fun `WHEN BleSensorDataDto has an explicit timestamp THEN it carries it through serialization`() {
        val dto = BleSensorDataDto(sensorTypeUid = 1L, value = JsonPrimitive(23.5), timestampMs = 1000L)

        val decoded = json.decodeFromString<BleSensorDataDto>(json.encodeToString(dto))

        assertEquals(1000L, decoded.timestampMs)
    }

    @Test
    fun `WHEN BleTaskStateEventPayload is serialized without optional fields THEN it deserializes with the default values`() {
        val payload = BleTaskStateEventPayload(taskTypeUid = 1L, state = "RUNNING")

        val decoded = json.decodeFromString<BleTaskStateEventPayload>(json.encodeToString(payload))

        assertEquals(payload, decoded)
        assertEquals(0L, decoded.taskUid)
        assertEquals(0.0, decoded.progress)
        assertEquals("", decoded.errorMessage)
    }

    @Test
    fun `WHEN BleTaskStateEventPayload has explicit optional fields THEN it carries them through serialization`() {
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
