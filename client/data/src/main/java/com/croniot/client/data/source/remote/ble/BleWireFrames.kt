package com.croniot.client.data.source.remote.ble

import croniot.models.dto.SensorTypeDto
import croniot.models.dto.TaskTypeDto
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

// {"deviceId":"<uuid>","protocolVersion":1,"schemaVersion":<uint32>}
@Serializable
data class BleDeviceInfo(
    val deviceId: String,
    val protocolVersion: Int,
    val schemaVersion: Long,
)

@Serializable
data class BleAuthRequest(val username: String, val password: String)

@Serializable
data class BleAuthResponse(val ok: Boolean, val error: String? = null)

@Serializable
data class BleRequestSyncPayload(val taskTypeUid: Long)

// Matches the schema JSON sent by ESP32 via SyncData chunks:
// {"sensorTypes":[...],"taskTypes":[...]}
@Serializable
data class BleSchemaDto(
    val sensorTypes: List<SensorTypeDto>,
    val taskTypes: List<TaskTypeDto>,
)

// Sensor notification from ESP32 via CHARACTERISTIC_SENSORS.
// value accepts both JSON string ("23.5") and number (23.5).
// timestampMs is optional — Android uses current time if absent.
// Example: {"sensorTypeUid":1,"value":23.5}
@Serializable
data class BleSensorDataDto(
    val sensorTypeUid: Long,
    val value: JsonElement,
    val timestampMs: Long? = null,
)

// Task state event from ESP32 via CHARACTERISTIC_TASK_PROGRESS.
// Android injects deviceUuid and defaults taskUid=0 for BLE.
// Example: {"type":"stateInfoEvent","data":{"taskTypeUid":1,"state":"RUNNING"}}
@Serializable
data class BleTaskStateEventPayload(
    val taskTypeUid: Long,
    val taskUid: Long = 0,
    val state: String,
    val progress: Double = 0.0,
    val errorMessage: String = "",
    val timestampMs: Long? = null,
)
