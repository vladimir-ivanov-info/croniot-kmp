package com.croniot.client.data.source.remote.http.sensors

import com.croniot.client.core.models.SensorData
import com.croniot.client.data.sensors.datasource.LocalSensorDataSource
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.data.strategy.DataSourceStrategy
import com.croniot.client.data.strategy.DataSourceStrategyBus
import croniot.messages.MessageLoginRequest
import croniot.models.LoginResultDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


class StrategyLocalSensorDataSource(
    private val realLocalSensorDataSource: LocalSensorDataSource,
    private val demoLocalSensorDataSource: LocalSensorDataSource,
    private val bus: DataSourceStrategyBus
) : LocalSensorDataSource {

    private fun getCurrentStrategy(): LocalSensorDataSource {
        val active = if (bus.current.value == DataSourceStrategy.DEMO) demoLocalSensorDataSource else realLocalSensorDataSource
        return active
    }

    override suspend fun save(sensorData: SensorData) {
        getCurrentStrategy().save(sensorData)
    }

    override suspend fun getLatest(
        deviceUuid: String,
        sensorTypeUid: Long,
        limit: Int
    ): List<SensorData> {
        return getCurrentStrategy().getLatest(
            deviceUuid = deviceUuid,
            sensorTypeUid = sensorTypeUid,
            limit = limit
        )
    }

    override /*suspend*/ fun observeSensorData(
        deviceUuid: String,
        sensorTypeUid: Long
    ): /*State*/Flow<SensorData> {
        return getCurrentStrategy().observeSensorData(
            deviceUuid = deviceUuid,
            sensorTypeUid = sensorTypeUid
        )
    }


}