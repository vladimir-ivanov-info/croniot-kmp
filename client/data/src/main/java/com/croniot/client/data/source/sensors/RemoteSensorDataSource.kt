package com.croniot.client.data.source.sensors

import com.croniot.client.core.models.SensorData

interface RemoteSensorDataSource {

    suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit,
    )


    suspend fun stopListening(deviceUuid: String? = null)
}
