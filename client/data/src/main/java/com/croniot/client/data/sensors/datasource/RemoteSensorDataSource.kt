package com.croniot.client.data.sensors.datasource

import com.croniot.client.core.models.SensorData

interface RemoteSensorDataSource {

    suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit,
    ) // inicia la suscripción remota

    // val incomingData: Flow<SensorData> // stream remoto de lecturas
    suspend fun stopListening(deviceUuid: String? = null) // opcional
}
