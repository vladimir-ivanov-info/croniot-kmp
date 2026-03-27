package com.croniot.client.domain.repositories

import Outcome
import com.croniot.client.core.models.ConnectionError
import com.croniot.client.core.models.Device
import com.croniot.client.core.models.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SensorDataRepository {
    val devicesLatestSensorTimestamp: StateFlow<Map<String, Long>>

    suspend fun listenToDeviceSensors(device: Device): Outcome<Unit, ConnectionError>

    suspend fun stopAllListeners()

    fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): /*State*/Flow<SensorData>
    suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int): List<SensorData>
}
