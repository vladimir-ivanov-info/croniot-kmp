package com.croniot.client.domain.usecases.ble

import Outcome
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.repositories.BleDevicesRepository

class PairBleDeviceUseCase(
    private val bleDevicesRepository: BleDevicesRepository,
) {

    suspend operator fun invoke(
        deviceUuid: String,
        username: String,
        password: String,
    ): Outcome<Device, BleError> =
        bleDevicesRepository.pair(deviceUuid, username, password)
}
