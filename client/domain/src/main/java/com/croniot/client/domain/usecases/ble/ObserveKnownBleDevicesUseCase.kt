package com.croniot.client.domain.usecases.ble

import com.croniot.client.domain.models.ble.KnownBleDevice
import com.croniot.client.domain.repositories.BleDevicesRepository
import kotlinx.coroutines.flow.Flow

class ObserveKnownBleDevicesUseCase(
    private val bleDevicesRepository: BleDevicesRepository,
) {

    operator fun invoke(): Flow<List<KnownBleDevice>> =
        bleDevicesRepository.observeKnownDevices()
}
