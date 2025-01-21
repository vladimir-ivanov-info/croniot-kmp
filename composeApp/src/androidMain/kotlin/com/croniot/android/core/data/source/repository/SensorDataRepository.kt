package com.croniot.android.core.data.source.repository

import croniot.models.dto.DeviceDto
import croniot.models.dto.SensorDataDto
import croniot.models.dto.SensorTypeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface SensorDataRepository {

    fun getStateFlow() : StateFlow<Map<SensorTypeDto, MutableStateFlow<SensorDataDto>>>

    suspend fun listenToDeviceSensors(device: DeviceDto)
}