package com.croniot.client.features.sensors.data.datasource.local

import com.croniot.android.core.data.entities.SensorDataRealm
import com.croniot.client.core.models.SensorData
import com.croniot.client.data.mappers.toAndroidModel
import com.croniot.client.data.sensors.datasource.LocalSensorDataSource
import com.croniot.client.data.source.local.RealmRef
import io.realm.kotlin.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

class LocalSensorDataSourceImpl : LocalSensorDataSource {

    private val _latestSensorData = mutableMapOf<Pair<String, Long>, MutableStateFlow<SensorData>>()

    private val realm: Realm = RealmRef.realmRef //TODO inject correctly

    override suspend fun save(sensorData: SensorData) {
        withContext(Dispatchers.IO) {
            realm.writeBlocking {
                copyToRealm(
                    SensorDataRealm().apply {
                        deviceUuid = sensorData.deviceUuid
                        sensorTypeUid = sensorData.sensorTypeUid
                        value = sensorData.value
                        timeStampMillis = sensorData.timeStamp.toInstant().toEpochMilli()
                    }
                )
            }
        }
    }

    override suspend fun getLatest(deviceUuid: String, sensorTypeUid: Long, limit: Int): List<SensorData> {
        return withContext(Dispatchers.IO) {
            realm.query(
                SensorDataRealm::class,
                "deviceUuid == $0 AND sensorTypeUid == $1 SORT(timeStampMillis DESC) LIMIT($limit)",
                deviceUuid, sensorTypeUid
            ).find().map { it.toAndroidModel() }
        }
    }


    override /*suspend*/ fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): /*State*/Flow<SensorData> {
        val key = deviceUuid to sensorTypeUid

        return _latestSensorData.getOrPut(key) {
            MutableStateFlow(
                SensorData(deviceUuid, sensorTypeUid, "0", ZonedDateTime.now())
            ).also { stateFlow -> //TODO the zero
                CoroutineScope(Dispatchers.IO).launch {
                    realm.query(
                        SensorDataRealm::class,
                        "deviceUuid == $0 AND sensorTypeUid == $1 SORT(timeStampMillis DESC) LIMIT(1)", //TODO MAX(timestampMillis)
                        deviceUuid,
                        sensorTypeUid,
                    ).asFlow()
                        .map { it.list.maxByOrNull { it.timeStampMillis }?.toAndroidModel() }
                        .filterNotNull()
                        .distinctUntilChanged()
                        .collect { latestValue ->
                            stateFlow.value = latestValue
                        }
                }
            }
        }
    }

}