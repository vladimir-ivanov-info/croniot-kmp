package com.croniot.client.data.source.sensors

import com.croniot.client.core.models.SensorData
import kotlinx.coroutines.flow.Flow

interface LocalSensorDataSource {

    suspend fun save(sensorData: SensorData)

    suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, limit: Int): List<SensorData>

    fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): Flow<SensorData> // TODO remove sensorTypeUid and listen to all sensors
}
