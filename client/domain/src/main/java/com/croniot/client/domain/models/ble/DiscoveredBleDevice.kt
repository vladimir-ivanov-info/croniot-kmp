package com.croniot.client.domain.models.ble

data class DiscoveredBleDevice(
    val uuid: String,
    val displayName: String,
    val rssi: Int,
    val isPaired: Boolean,
)
