package com.croniot.android.core.data.source.repository

import croniot.models.dto.SensorTypeDto

interface SensorDataRepository {

    suspend fun listenToSensor(deviceUuid: String, sensorType: SensorTypeDto)
}