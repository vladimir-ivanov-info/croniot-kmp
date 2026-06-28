package com.croniot.client.domain.models.ble

data class KnownBleDevice(
    val uuid: String,
    val displayName: String,
    val lastSeenAtMillis: Long,
    val isInRange: Boolean,
)
