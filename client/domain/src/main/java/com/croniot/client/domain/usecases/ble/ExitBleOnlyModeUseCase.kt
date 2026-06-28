package com.croniot.client.domain.usecases.ble

import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.BleDevicesRepository

class ExitBleOnlyModeUseCase(
    private val bleDevicesRepository: BleDevicesRepository,
    private val appSessionRepository: AppSessionRepository,
) {

    suspend operator fun invoke() {
        bleDevicesRepository.disconnectAll()
        appSessionRepository.clear()
    }
}
