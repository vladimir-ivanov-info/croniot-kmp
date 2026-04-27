package com.croniot.client.data.source.remote.ble

import java.util.UUID

// Los UUIDs deben coincidir con el firmware ESP32. Cambios aquí requieren cambio en firmware.
object BleProfile {
    val SERVICE_UUID: UUID = UUID.fromString("9bc60001-7eee-4d11-9e8e-c0c011e00001")
    val CHARACTERISTIC_AUTH: UUID = UUID.fromString("9bc60001-7eee-4d11-9e8e-c0c011e00002")
    val CHARACTERISTIC_SENSORS: UUID = UUID.fromString("9bc60001-7eee-4d11-9e8e-c0c011e00003")
    val CHARACTERISTIC_TASKS: UUID = UUID.fromString("9bc60001-7eee-4d11-9e8e-c0c011e00004")
    val CHARACTERISTIC_DEVICE_INFO: UUID = UUID.fromString("9bc60001-7eee-4d11-9e8e-c0c011e00005")

    const val ADVERTISED_DEVICE_UUID_KEY = "uuid"
    const val DEFAULT_OPERATION_TIMEOUT_MS = 8_000L
}
