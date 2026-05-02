package com.croniot.client.domain.usecases.ble

import com.croniot.client.domain.repositories.BleDevicesRepository
import kotlinx.coroutines.flow.Flow

class ObserveBleRssiUseCase(
    private val bleDevicesRepository: BleDevicesRepository,
) {

    operator fun invoke(deviceUuid: String): Flow<Int?> =
        bleDevicesRepository.observeRssi(deviceUuid)
}
