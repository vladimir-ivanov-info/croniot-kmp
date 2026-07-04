package com.croniot.client.data.source.local

import com.croniot.client.domain.models.Device

interface DeviceLocalDatasource {
    suspend fun getLocalDeviceUuid(): String?
    suspend fun getLocalDeviceToken(): String?
    suspend fun generateAndSaveDeviceUuidIfNotExists()
    suspend fun getSelectedDevice(): Device?
    suspend fun saveSelectedDevice(device: Device)
}
