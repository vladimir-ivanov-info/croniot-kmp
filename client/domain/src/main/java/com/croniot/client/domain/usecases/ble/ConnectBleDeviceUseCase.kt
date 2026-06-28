package com.croniot.client.domain.usecases.ble

import Outcome
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.repositories.BleDevicesRepository

class ConnectBleDeviceUseCase(
    private val bleDevicesRepository: BleDevicesRepository,
) {

    suspend operator fun invoke(deviceUuid: String): Outcome<Device, BleError> =
        bleDevicesRepository.connect(deviceUuid)
}
