package com.croniot.client.domain.usecases.ble

import com.croniot.client.domain.repositories.BleDevicesRepository

class ForgetBleDeviceUseCase(
    private val bleDevicesRepository: BleDevicesRepository,
) {

    suspend operator fun invoke(deviceUuid: String) {
        bleDevicesRepository.forget(deviceUuid)
    }
}
