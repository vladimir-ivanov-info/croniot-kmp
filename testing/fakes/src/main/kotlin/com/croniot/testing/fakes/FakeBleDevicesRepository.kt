package com.croniot.testing.fakes

import Outcome
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.ble.DiscoveredBleDevice
import com.croniot.client.domain.models.ble.KnownBleDevice
import com.croniot.client.domain.repositories.BleDevicesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBleDevicesRepository(
    devicesByUuid: Map<String, Device> = emptyMap(),
) : BleDevicesRepository {

    private val devicesByUuid: MutableMap<String, Device> = devicesByUuid.toMutableMap()
    private val nearbyDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    private val knownDevices = MutableStateFlow<List<KnownBleDevice>>(emptyList())

    var getDeviceInvocations: MutableList<String> = mutableListOf()
        private set

    var forgetCalls: Int = 0
        private set

    var disconnectAllCalls: Int = 0
        private set

    override fun observeNearbyDevices(): Flow<List<DiscoveredBleDevice>> = nearbyDevices

    override fun observeKnownDevices(): Flow<List<KnownBleDevice>> = knownDevices

    override fun observeRssi(deviceUuid: String): Flow<Int?> = MutableStateFlow(null)

    override suspend fun pair(deviceUuid: String, username: String, password: String): Outcome<Device, BleError> =
        devicesByUuid[deviceUuid]?.let { Outcome.Ok(it) } ?: Outcome.Err(BleError.Unknown(null))

    override suspend fun connect(deviceUuid: String): Outcome<Device, BleError> =
        devicesByUuid[deviceUuid]?.let { Outcome.Ok(it) } ?: Outcome.Err(BleError.Unknown(null))

    override suspend fun getDevice(deviceUuid: String): Device? {
        getDeviceInvocations += deviceUuid
        return devicesByUuid[deviceUuid]
    }

    override suspend fun forget(deviceUuid: String) {
        forgetCalls++
        devicesByUuid.remove(deviceUuid)
    }

    override suspend fun disconnectAll() {
        disconnectAllCalls++
    }
}
