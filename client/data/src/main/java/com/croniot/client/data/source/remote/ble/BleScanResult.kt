package com.croniot.client.data.source.remote.ble

data class BleScanResult(
    val macAddress: String,
    val advertisedName: String?,
    val rssi: Int,
)
