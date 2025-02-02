package com.croniot.android.features.device.features.sensors.presentation

import androidx.lifecycle.ViewModel
import com.croniot.android.core.data.source.repository.SensorDataRepository
import croniot.models.dto.SensorDataDto
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ViewModelSensors() : ViewModel(), KoinComponent {

    private val SENSOR_DATA_CACHE_SIZE = 50

    private val sensorDataRepositoryImpl: SensorDataRepository = get()

    suspend fun getInitialChartData(sensorUid: Long, deviceUuid: String): List<SensorDataDto> {
        val listSensorDataDto = sensorDataRepositoryImpl.getLatestSensorData(deviceUuid, sensorUid, SENSOR_DATA_CACHE_SIZE)
        return listSensorDataDto
    }

    fun observeLiveSensorData(sensorUid: Long, deviceUuid: String): StateFlow<SensorDataDto> {
        return sensorDataRepositoryImpl.observeSensorData(deviceUuid, sensorUid)
    }
}