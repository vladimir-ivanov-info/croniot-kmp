package com.croniot.client.data.repositories

import com.croniot.client.core.models.Device
import com.croniot.client.data.source.sensors.LocalSensorDataSource
import com.croniot.client.data.source.sensors.RemoteSensorDataSource
import com.croniot.client.domain.repositories.SensorDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class SensorDataRepositoryImpl(
    private val remoteSensorDataSource: RemoteSensorDataSource,
    private val localSensorDataSource: LocalSensorDataSource,
    private val scope: CoroutineScope,
) : SensorDataRepository {
    private val _devicesLatestSensorTimestamp = MutableStateFlow<Map<String, Long>>(emptyMap())
    override val devicesLatestSensorTimestamp: StateFlow<Map<String, Long>> = _devicesLatestSensorTimestamp

    override suspend fun stopAllListeners() {
        remoteSensorDataSource.stopListening(deviceUuid = null)
        _devicesLatestSensorTimestamp.value = emptyMap()
        scope.coroutineContext.cancelChildren()
    }

    override suspend fun listenToDeviceSensors(device: Device) {
        remoteSensorDataSource.listenDeviceSensors(
            deviceUuid = device.uuid,
            onNewSensorData = { sensorData ->
                scope.launch {
                    localSensorDataSource.save(sensorData)

                    _devicesLatestSensorTimestamp.update { oldMap ->
                        oldMap + (device.uuid to ZonedDateTime.now().toInstant().toEpochMilli())
                    }
                }
            },
        )
    }

    override suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int) =
        localSensorDataSource.getLatestSensorData(deviceUuid, sensorTypeUid, elements)

    override fun observeSensorData(deviceUuid: String, sensorTypeUid: Long) =
        localSensorDataSource.observeSensorData(deviceUuid, sensorTypeUid)
}
