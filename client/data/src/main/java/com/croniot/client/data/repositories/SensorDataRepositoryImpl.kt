package com.croniot.client.data.repositories

import Outcome
import com.croniot.client.data.source.sensors.LocalSensorDataSource
import com.croniot.client.data.source.sensors.RemoteSensorDataSource
import com.croniot.client.data.source.transport.TransportRouter
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.TransportKind
import com.croniot.client.domain.repositories.SensorDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class SensorDataRepositoryImpl(
    private val cloudSensorDataSource: RemoteSensorDataSource,
    private val bleSensorDataSource: RemoteSensorDataSource,
    private val transportRouter: TransportRouter,
    private val localSensorDataSource: LocalSensorDataSource,
    private val scope: CoroutineScope,
) : SensorDataRepository {
    private val _devicesLatestSensorTimestamp = MutableStateFlow<Map<String, Long>>(emptyMap())
    override val devicesLatestSensorTimestamp: StateFlow<Map<String, Long>> = _devicesLatestSensorTimestamp

    private fun dataSourceFor(deviceUuid: String): RemoteSensorDataSource =
        when (transportRouter.transportFor(deviceUuid)) {
            TransportKind.BLE -> bleSensorDataSource
            TransportKind.CLOUD -> cloudSensorDataSource
        }

    override suspend fun stopAllListeners() {
        cloudSensorDataSource.stopListening(deviceUuid = null)
        bleSensorDataSource.stopListening(deviceUuid = null)
        _devicesLatestSensorTimestamp.value = emptyMap()
        scope.coroutineContext.cancelChildren()
    }

    override suspend fun listenToDeviceSensors(device: Device): Outcome<Unit, ConnectionError> {
        return dataSourceFor(device.uuid).listenDeviceSensors(
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
