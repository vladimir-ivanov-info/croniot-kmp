package com.croniot.client.data.source.remote.ble

import java.util.UUID

// Los UUIDs deben coincidir con el firmware ESP32.
object BleProfile {
    val SERVICE_UUID: UUID = UUID.fromString("5e6f0001-7c7b-4f6e-9e8a-2a3a1b2c3d4e")
    
    val CHARACTERISTIC_DEVICE_INFO: UUID = UUID.fromString("5e6f0002-7c7b-4f6e-9e8a-2a3a1b2c3d4e")
    val CHARACTERISTIC_AUTH: UUID = UUID.fromString("5e6f0003-7c7b-4f6e-9e8a-2a3a1b2c3d4e")
    val CHARACTERISTIC_SENSORS: UUID = UUID.fromString("5e6f0004-7c7b-4f6e-9e8a-2a3a1b2c3d4e")
    
    // El firmware separa el progreso de los comandos y la sincronización
    val CHARACTERISTIC_TASK_PROGRESS: UUID = UUID.fromString("5e6f0005-7c7b-4f6e-9e8a-2a3a1b2c3d4e")
    val CHARACTERISTIC_TASK_COMMAND: UUID = UUID.fromString("5e6f0006-7c7b-4f6e-9e8a-2a3a1b2c3d4e")
    val CHARACTERISTIC_TASK_STATE_SYNC: UUID = UUID.fromString("5e6f0007-7c7b-4f6e-9e8a-2a3a1b2c3d4e")
    val CHARACTERISTIC_SYNC_COMMAND: UUID = UUID.fromString("5e6f0008-7c7b-4f6e-9e8a-2a3a1b2c3d4e")
    val CHARACTERISTIC_SYNC_DATA: UUID = UUID.fromString("5e6f0009-7c7b-4f6e-9e8a-2a3a1b2c3d4e")

    const val ADVERTISED_DEVICE_UUID_KEY = "uuid"
    const val DEFAULT_OPERATION_TIMEOUT_MS = 8_000L
    const val SYNC_CHUNK_TIMEOUT_MS = 5_000L
}
