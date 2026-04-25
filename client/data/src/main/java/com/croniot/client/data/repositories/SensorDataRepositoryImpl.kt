package com.croniot.client.data.repositories

import Outcome
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.Device
import com.croniot.client.data.source.sensors.LocalSensorDataSource
import com.croniot.client.data.source.sensors.RemoteSensorDataSource
import com.croniot.client.domain.repositories.SensorDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime

class SensorDataRepositoryImpl(
    private val remoteSensorDataSource: RemoteSensorDataSource,
    private val localSensorDataSource: LocalSensorDataSource,
    private val scope: CoroutineScope,
) : SensorDataRepository {
    private val _devicesLatestSensorTimestamp = MutableStateFlow<Map<String, Long>>(emptyMap())
    override val devicesLatestSensorTimestamp: StateFlow<Map<String, Long>> = _devicesLatestSensorTimestamp

    override suspend fun stopListeningFor(deviceUuid: String) {
        remoteSensorDataSource.stopListening(deviceUuid)
        _devicesLatestSensorTimestamp.update { it - deviceUuid }
    }

    override suspend fun stopAllListeners() {
        remoteSensorDataSource.stopListening(deviceUuid = null)
        _devicesLatestSensorTimestamp.value = emptyMap()
        scope.coroutineContext.cancelChildren()
    }

    override suspend fun listenToDeviceSensors(device: Device): Outcome<Unit, ConnectionError> {
        return when (val outcome = remoteSensorDataSource.listenDeviceSensors(device.uuid)) {
            is Outcome.Ok -> {
                outcome.value
                    .onEach { sensorData ->
                        localSensorDataSource.save(sensorData)
                        _devicesLatestSensorTimestamp.update { oldMap ->
                            oldMap + (device.uuid to ZonedDateTime.now().toInstant().toEpochMilli())
                        }
                    }
                    .launchIn(scope)
                Outcome.Ok(Unit)
            }
            is Outcome.Err -> outcome
        }
    }

    override suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int) =
        localSensorDataSource.getLatestSensorData(deviceUuid, sensorTypeUid, elements)

    override fun observeSensorData(deviceUuid: String, sensorTypeUid: Long) =
        localSensorDataSource.observeSensorData(deviceUuid, sensorTypeUid)
}
