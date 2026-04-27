package com.croniot.client.data.source.remote.ble

data class BleScanResult(
    val deviceUuid: String,
    val advertisedName: String?,
    val macAddress: String,
    val rssi: Int,
)
