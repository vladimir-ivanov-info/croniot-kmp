package com.croniot.client.data.source.sensors

import Outcome
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.SensorData
import kotlinx.coroutines.flow.Flow

interface RemoteSensorDataSource {

    suspend fun listenDeviceSensors(
        deviceUuid: String,
    ): Outcome<Flow<SensorData>, ConnectionError>

    suspend fun stopListening(deviceUuid: String? = null)
}
