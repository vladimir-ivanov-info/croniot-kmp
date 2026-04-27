package com.croniot.client.data.source.remote.ble

import croniot.models.TaskKey
import croniot.models.dto.TaskStateInfoDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Envelope para multiplexar mensajes en CHARACTERISTIC_TASKS.
// El firmware ESP32 debe usar el mismo formato.
@Serializable
data class BleTaskFrame(
    val type: String,
    val data: JsonElement,
)

object BleTaskFrameType {
    const val ADD_TASK = "addTask"
    const val REQUEST_SYNC = "requestSync"
    const val NEW_TASK = "newTask"
    const val STATE_INFO_EVENT = "stateInfoEvent"
}

@Serializable
data class BleAuthRequest(val username: String, val password: String)

@Serializable
data class BleAuthResponse(val ok: Boolean, val error: String? = null)

@Serializable
data class BleRequestSyncPayload(val taskTypeUid: Long)

@Serializable
data class BleStateInfoEventPayload(
    val key: TaskKey,
    val info: TaskStateInfoDto,
)
