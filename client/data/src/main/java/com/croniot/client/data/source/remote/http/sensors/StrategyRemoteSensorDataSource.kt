package com.croniot.client.data.source.remote.http.sensors

import com.croniot.client.core.models.SensorData
import com.croniot.client.data.sensors.datasource.RemoteSensorDataSource
import com.croniot.client.data.strategy.DataSourceStrategy
import com.croniot.client.data.strategy.DataSourceStrategyBus

class StrategyRemoteSensorDataSource(
    private val realRemoteSensorDataSource: RemoteSensorDataSource,
    private val demoRemoteSensorDataSource: RemoteSensorDataSource,
    private val bus: DataSourceStrategyBus,
) : RemoteSensorDataSource {

    private fun getCurrentStrategy(): RemoteSensorDataSource {
        val active = if (bus.current.value == DataSourceStrategy.DEMO) demoRemoteSensorDataSource else realRemoteSensorDataSource
        return active
    }

    override suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit,
    ) {
        getCurrentStrategy().listenDeviceSensors(
            deviceUuid = deviceUuid,
            onNewSensorData = onNewSensorData,
        )
    }

    override suspend fun stopListening(deviceUuid: String?) {
        getCurrentStrategy().stopListening(deviceUuid = deviceUuid)
    }
}
