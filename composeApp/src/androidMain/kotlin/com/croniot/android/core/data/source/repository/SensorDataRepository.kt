package com.croniot.android.core.data.source.repository

import com.croniot.android.domain.model.Device
import com.croniot.android.domain.model.SensorData
import kotlinx.coroutines.flow.StateFlow

interface SensorDataRepository {

    suspend fun listenToDeviceSensors(device: Device)

    fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): StateFlow<SensorData>
    suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int): List<SensorData>

    fun observeSensorDataInsertions(deviceUuid: String): StateFlow<Long>

}
