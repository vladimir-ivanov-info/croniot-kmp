package com.croniot.client.data.sensors.datasource

import com.croniot.client.core.models.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LocalSensorDataSource {

    suspend fun save(sensorData: SensorData)

    suspend fun getLatest(deviceUuid: String, sensorTypeUid: Long, limit: Int) : List<SensorData>

    //suspend fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): StateFlow<SensorData> //TODO remove sensorTypeUid and listen to all sensors
    /*suspend*/ fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): Flow<SensorData> //TODO remove sensorTypeUid and listen to all sensors

}