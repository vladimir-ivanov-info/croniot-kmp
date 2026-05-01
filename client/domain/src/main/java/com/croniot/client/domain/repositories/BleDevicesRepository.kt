package com.croniot.client.domain.repositories

import Outcome
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.ble.DiscoveredBleDevice
import com.croniot.client.domain.models.ble.KnownBleDevice
import kotlinx.coroutines.flow.Flow

interface BleDevicesRepository {

    fun observeNearbyDevices(): Flow<List<DiscoveredBleDevice>>

    fun observeKnownDevices(): Flow<List<KnownBleDevice>>

    suspend fun pair(
        deviceUuid: String,
        username: String,
        password: String,
    ): Outcome<Device, BleError>

    suspend fun connect(deviceUuid: String): Outcome<Device, BleError>

    suspend fun getDevice(deviceUuid: String): Device?

    suspend fun forget(deviceUuid: String)

    suspend fun disconnectAll()
}
