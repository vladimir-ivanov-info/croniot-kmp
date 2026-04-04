package com.croniot.client.data.source.sensors

import Outcome
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.SensorData

interface RemoteSensorDataSource {

    suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit,
    ): Outcome<Unit, ConnectionError>

    suspend fun stopListening(deviceUuid: String? = null)
}
