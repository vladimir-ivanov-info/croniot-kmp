package com.croniot.client.data.source.sensors

import Outcome
import com.croniot.client.data.source.remote.ble.BleConnectionPool
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BleSensorDataSourceImpl(
    private val appScope: CoroutineScope,
    private val connectionPool: BleConnectionPool,
) : RemoteSensorDataSource {

    private val collectorJobs = ConcurrentHashMap<String, Job>()

    override suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit,
    ): Outcome<Unit, ConnectionError> {
        val connection = connectionPool.get(deviceUuid)
            ?: return Outcome.Err(ConnectionError.Unknown)

        collectorJobs.remove(deviceUuid)?.cancel()
        val job = appScope.launch {
            connection.observeSensorData().collect { onNewSensorData(it) }
        }
        collectorJobs[deviceUuid] = job
        return Outcome.Ok(Unit)
    }

    override suspend fun stopListening(deviceUuid: String?) {
        if (deviceUuid != null) {
            collectorJobs.remove(deviceUuid)?.cancel()
        } else {
            collectorJobs.values.forEach { it.cancel() }
            collectorJobs.clear()
        }
    }
}
