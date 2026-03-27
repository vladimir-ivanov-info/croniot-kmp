package com.croniot.client.data.source.sensors

import Outcome
import com.croniot.client.core.models.ConnectionError
import com.croniot.client.core.models.SensorData

interface RemoteSensorDataSource {

    suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit,
    ): Outcome<Unit, ConnectionError>

    suspend fun stopListening(deviceUuid: String? = null)
}
