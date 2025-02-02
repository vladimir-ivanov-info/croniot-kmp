package com.croniot.android.core.data.source.repository

import croniot.models.dto.DeviceDto
import croniot.models.dto.SensorDataDto
import kotlinx.coroutines.flow.StateFlow

interface SensorDataRepository {

    suspend fun listenToDeviceSensors(device: DeviceDto)

    fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): StateFlow<SensorDataDto>
    suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int): List<SensorDataDto>
}
