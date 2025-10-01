package com.croniot.client.features.sensors.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.Constants
import com.croniot.client.core.models.SensorData
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.core.models.Device
import kotlinx.coroutines.flow.SharingStarted
/*import com.croniot.android.core.data.source.repository.SensorDataRepository
import com.croniot.android.domain.model.SensorData*/
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.time.ZonedDateTime

class SensorsViewModel(
    private val sensorDataRepository: SensorDataRepository
) : ViewModel(), KoinComponent {

    private val SENSOR_DATA_CACHE_SIZE = 50

    private data class Key(val deviceUuid: String, val sensorUid: Long)

    private val liveFlows = mutableMapOf<Key, StateFlow<SensorData>>()
    private val historyCache = mutableMapOf<Key, List<SensorData>>() // opcional: cache de histórico


    //TODO delegate data saving into viewmodel and observe it from UI

    suspend fun getInitialChartData(sensorUid: Long, deviceUuid: String): List<SensorData> {
        //val listSensorDataDto = sensorDataRepositoryImpl.getLatestSensorData(deviceUuid, sensorUid, SENSOR_DATA_CACHE_SIZE)
        //return listSensorDataDto

        val key = Key(deviceUuid, sensorUid)
        return historyCache.getOrPut(key) {
            sensorDataRepository.getLatestSensorData(deviceUuid, sensorUid, SENSOR_DATA_CACHE_SIZE)
        }
    }

    /*suspend fun observeLiveSensorData(sensorUid: Long, deviceUuid: String): StateFlow<SensorData> {
        return sensorDataRepositoryImpl.observeSensorData(deviceUuid, sensorUid).stateIn(
            scope = viewModelScope,
        )
    }*/

    fun listenSensorData(sensorUid: Long, deviceUuid: String): StateFlow<SensorData> {
        val key = Key(deviceUuid, sensorUid)
        return liveFlows.getOrPut(key) {
            // Flow -> StateFlow con valor inicial
            sensorDataRepository.observeSensorData(deviceUuid, sensorUid)
                .onEach { item ->
                    println(item) //TODO
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = SensorData(
                        deviceUuid = deviceUuid,
                        sensorTypeUid = sensorUid,
                        value = Constants.PARAMETER_VALUE_UNDEFINED, //TODO
                        timeStamp = ZonedDateTime.now()
                    )
                )
        }
    }

}
