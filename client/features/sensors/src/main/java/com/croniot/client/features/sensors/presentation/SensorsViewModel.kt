package com.croniot.client.features.sensors.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.config.Constants
import com.croniot.client.core.models.SensorData
import com.croniot.client.core.models.SensorType
import com.croniot.client.domain.repositories.SensorDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

class SensorsViewModel(
    private val sensorDataRepository: SensorDataRepository,
) : ViewModel(), KoinComponent {

    companion object {
        private const val SENSOR_DATA_CACHE_SIZE = 50
    }

    private data class Key(val deviceUuid: String, val sensorUid: Long)

    private val liveFlows = ConcurrentHashMap<Key, StateFlow<SensorData>>()
    private val historyCache = ConcurrentHashMap<Key, List<SensorData>>()

    // TODO delegate data saving into viewmodel and observe it from UI

    private val _sensorsInitialData = MutableStateFlow<Map<Long, List<SensorData>>>(emptyMap())
    val sensorsInitialData: StateFlow<Map<Long, List<SensorData>>> = _sensorsInitialData

    fun loadAllInitialData(deviceUuid: String, sensorTypes: List<SensorType>) {
        viewModelScope.launch(Dispatchers.IO) {
            sensorTypes.map { sensorType ->
                async {
                    val data = getInitialChartData(deviceUuid, sensorType.uid)
                    _sensorsInitialData.update { it + (sensorType.uid to data) }
                }
            }.awaitAll()
        }
    }

    private suspend fun getInitialChartData(deviceUuid: String, sensorUid: Long): List<SensorData> {
        val key = Key(deviceUuid, sensorUid)
        return historyCache.getOrPut(key) {
            sensorDataRepository.getLatestSensorData(deviceUuid, sensorUid, SENSOR_DATA_CACHE_SIZE)
        }
    }

    fun listenSensorData(sensorUid: Long, deviceUuid: String): StateFlow<SensorData> {
        val key = Key(deviceUuid, sensorUid)
        return liveFlows.getOrPut(key) {
            // Flow -> StateFlow con valor inicial
            sensorDataRepository.observeSensorData(deviceUuid, sensorUid)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = SensorData(
                        deviceUuid = deviceUuid,
                        sensorTypeUid = sensorUid,
                        value = Constants.PARAMETER_VALUE_UNDEFINED, // TODO
                        timeStamp = ZonedDateTime.now(),
                    ),
                )
        }
    }
}
