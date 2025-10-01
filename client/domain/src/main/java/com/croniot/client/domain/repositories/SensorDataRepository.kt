package com.croniot.client.domain.repositories

import com.croniot.client.core.models.Device
import com.croniot.client.core.models.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SensorDataRepository {
    val devicesLatestSensorTimestamp: StateFlow<Map<String, Long>>

    suspend fun listenToDeviceSensors(device: Device)

    /*suspend*/
    fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): /*State*/Flow<SensorData>
    suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int): List<SensorData>

    // fun observeSensorDataInsertions(deviceUuid: String): Flow<Long>
}
