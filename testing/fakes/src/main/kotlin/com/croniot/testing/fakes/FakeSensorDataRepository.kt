package com.croniot.testing.fakes

import Outcome
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.SensorData
import com.croniot.client.domain.repositories.SensorDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.util.concurrent.CopyOnWriteArrayList

class FakeSensorDataRepository(
    private val latestSensorDataByKey: Map<Pair<String, Long>, List<SensorData>> = emptyMap(),
    private val observeSensorDataByKey: Map<Pair<String, Long>, Flow<SensorData>> = emptyMap(),
    private val listenToDeviceSensorsOutcomeByDevice: Map<String, Outcome<Unit, ConnectionError>> = emptyMap(),
    private val listenToDeviceSensorsThrowsByDevice: Map<String, Throwable> = emptyMap(),
) : SensorDataRepository {

    private val latestSensorTimestamp = MutableStateFlow<Map<String, Long>>(emptyMap())

    var stopAllListenersCalls: Int = 0
        private set

    var listenToDeviceSensorsInvocations: MutableList<String> = CopyOnWriteArrayList()
        private set

    // CopyOnWriteArrayList: SensorsViewModel.loadAllInitialData fans out one coroutine per sensor type on
    // Dispatchers.IO (a real multi-threaded dispatcher), so these invocation lists are written concurrently
    // from different threads. A plain mutableListOf() here caused a real, intermittently-failing race
    // (writes from one thread not visible to the assertion thread).
    var stopListeningForInvocations: MutableList<String> = CopyOnWriteArrayList()
        private set

    var getLatestSensorDataInvocations: MutableList<Pair<String, Long>> = CopyOnWriteArrayList()
        private set

    var observeSensorDataInvocations: MutableList<Pair<String, Long>> = CopyOnWriteArrayList()
        private set

    override val devicesLatestSensorTimestamp: StateFlow<Map<String, Long>> = latestSensorTimestamp

    override suspend fun listenToDeviceSensors(device: Device): Outcome<Unit, ConnectionError> {
        listenToDeviceSensorsInvocations += device.uuid
        listenToDeviceSensorsThrowsByDevice[device.uuid]?.let { throw it }
        return listenToDeviceSensorsOutcomeByDevice[device.uuid] ?: Outcome.Ok(Unit)
    }

    override suspend fun stopListeningFor(deviceUuid: String) {
        stopListeningForInvocations += deviceUuid
    }

    override suspend fun stopAllListeners() {
        stopAllListenersCalls++
    }

    override fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): Flow<SensorData> {
        observeSensorDataInvocations += deviceUuid to sensorTypeUid
        return observeSensorDataByKey[deviceUuid to sensorTypeUid] ?: emptyFlow()
    }

    override suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int): List<SensorData> {
        getLatestSensorDataInvocations += deviceUuid to sensorTypeUid
        return latestSensorDataByKey[deviceUuid to sensorTypeUid] ?: emptyList()
    }
}
