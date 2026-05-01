package com.croniot.client.domain.usecases

import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.session.AppSession
import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.BleDevicesRepository

class GetDeviceUseCase(
    private val appSessionRepository: AppSessionRepository,
    private val bleDevicesRepository: BleDevicesRepository,
) {
    suspend operator fun invoke(deviceUuid: String): Device? =
        when (val session = appSessionRepository.session.value) {
            is AppSession.Server -> session.account.devices.find { it.uuid == deviceUuid }
            AppSession.BleOnly -> bleDevicesRepository.getDevice(deviceUuid)
            AppSession.None -> null
        }
}