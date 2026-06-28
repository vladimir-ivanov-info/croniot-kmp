package com.croniot.client.domain.usecases.ble

import com.croniot.client.domain.models.ble.DiscoveredBleDevice
import com.croniot.client.domain.repositories.BleDevicesRepository
import kotlinx.coroutines.flow.Flow

class ScanBleDevicesUseCase(
    private val bleDevicesRepository: BleDevicesRepository,
) {

    operator fun invoke(): Flow<List<DiscoveredBleDevice>> =
        bleDevicesRepository.observeNearbyDevices()
}
