package com.croniot.client.data.source.remote.ble

import kotlinx.coroutines.flow.Flow

interface BleScanner {
    fun scan(): Flow<List<BleScanResult>>
}
