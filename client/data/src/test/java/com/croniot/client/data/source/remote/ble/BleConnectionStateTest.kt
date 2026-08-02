package com.croniot.client.data.source.remote.ble

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BleConnectionStateTest {

    @Test
    fun `WHEN listing entries THEN it contains every connection state`() {
        assertEquals(
            listOf("Disconnected", "Connecting", "Connected", "Authenticating", "Ready", "Failed"),
            BleConnectionState.entries.map { it.name },
        )
    }

    @Test
    fun `WHEN valueOf is called with each state's name THEN it resolves that state`() {
        BleConnectionState.entries.forEach { state ->
            assertEquals(state, BleConnectionState.valueOf(state.name))
        }
    }
}

class BleSyncResultTest {

    @Test
    fun `WHEN accessing UpToDate THEN it is a singleton`() {
        val result: BleSyncResult = BleSyncResult.UpToDate

        assertTrue(result is BleSyncResult.UpToDate)
    }

    @Test
    fun `WHEN Updated is constructed THEN it carries the schema version and json payload`() {
        val result: BleSyncResult = BleSyncResult.Updated(schemaVersion = 3L, schemaJson = """{"a":1}""")

        assertTrue(result is BleSyncResult.Updated)
        result as BleSyncResult.Updated
        assertEquals(3L, result.schemaVersion)
        assertEquals("""{"a":1}""", result.schemaJson)
    }
}
