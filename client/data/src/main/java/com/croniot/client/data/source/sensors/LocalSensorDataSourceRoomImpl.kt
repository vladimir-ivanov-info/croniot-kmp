package com.croniot.client.data.source.sensors

import com.croniot.client.core.models.SensorData
import com.croniot.client.data.source.local.database.daos.SensorDataDao
import com.croniot.client.data.source.local.database.entities.SensorDataEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class LocalSensorDataSourceRoomImpl(
    private val sensorDataDao: SensorDataDao,
) : LocalSensorDataSource {

    override suspend fun save(sensorData: SensorData) {
        sensorDataDao.insert(sensorData.toEntity())
    }

    override suspend fun getLatestSensorData(
        deviceUuid: String,
        sensorTypeUid: Long,
        limit: Int,
    ): List<SensorData> {
        return sensorDataDao
            .getLatest(deviceUuid, sensorTypeUid, limit)
            .map { it.toModel() }
    }

    override fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): Flow<SensorData> {
        return sensorDataDao
            .observeLatest(deviceUuid, sensorTypeUid)
            .filterNotNull()
            .map { it.toModel() }
    }
}

private fun SensorData.toEntity() = SensorDataEntity(
    deviceUuid = deviceUuid,
    sensorTypeUid = sensorTypeUid,
    value = value,
    timeStampMillis = timeStamp.toInstant().toEpochMilli(),
)

private fun SensorDataEntity.toModel() = SensorData(
    deviceUuid = deviceUuid,
    sensorTypeUid = sensorTypeUid,
    value = value,
    timeStamp = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(timeStampMillis),
        ZoneOffset.UTC,
    ),
)
