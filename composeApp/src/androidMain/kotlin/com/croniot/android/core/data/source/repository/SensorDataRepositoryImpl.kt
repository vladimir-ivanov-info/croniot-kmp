package com.croniot.android.core.data.source.repository

import MqttHandler
import com.croniot.android.app.Global
import com.croniot.android.app.MyApp
import com.croniot.android.core.constants.ServerConfig
import com.croniot.android.core.data.entities.SensorDataRealm
import com.croniot.android.core.data.mappers.toAndroidModel
import com.croniot.android.domain.model.Device
import com.croniot.android.domain.model.SensorData
import com.croniot.android.features.device.features.sensors.data.processor.MqttProcessorSensorData
import croniot.models.dto.SensorTypeDto
import io.realm.kotlin.Realm
import io.realm.kotlin.notifications.UpdatedResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import java.time.ZonedDateTime

class SensorDataRepositoryImpl() : SensorDataRepository {

    private val realm: Realm = MyApp.realm

    private val sensorSharedFlows = mutableMapOf<SensorTypeDto, MutableSharedFlow<SensorData>>()
    private val _latestSensorData = mutableMapOf<Pair<String, Long>, MutableStateFlow<SensorData>>()

    override suspend fun listenToDeviceSensors(device: Device) {
        val clientId = ServerConfig.mqttClientId + Global.generateUniqueString(8)
        val mqttClient = MqttClient(ServerConfig.mqttBrokerUrl, clientId, null)

        val topic = "/server_to_app/${device.uuid}/sensor_data"
        MqttHandler(
            mqttClient,
            MqttProcessorSensorData(onNewData = { newSensorData ->
                CoroutineScope(Dispatchers.IO).launch {
                    realm.writeBlocking {
                        copyToRealm(
                            SensorDataRealm().apply {
                                deviceUuid = newSensorData.deviceUuid
                                sensorTypeUid = newSensorData.sensorTypeUid
                                value = newSensorData.value
                                timestampMillis = ZonedDateTime.now().toInstant().toEpochMilli()
                            },
                        )
                    }
                }
            }),
            topic,
        )
    }

    override suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int): List<SensorData> {
        return withContext(Dispatchers.IO) {
            realm.query(
                SensorDataRealm::class,
                "deviceUuid == $0 AND sensorTypeUid == $1 SORT(timestampMillis DESC) LIMIT($elements)",
                deviceUuid,
                sensorTypeUid,
            )
                .find()
                .map { it.toAndroidModel() }
        }
    }

    override fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): StateFlow<SensorData> {
        val key = deviceUuid to sensorTypeUid

        return _latestSensorData.getOrPut(key) {
            MutableStateFlow(SensorData(deviceUuid, sensorTypeUid, "0", ZonedDateTime.now())).also { stateFlow -> // TODO the zero
                CoroutineScope(Dispatchers.IO).launch {
                    realm.query(
                        SensorDataRealm::class,
                        "deviceUuid == $0 AND sensorTypeUid == $1 SORT(timestampMillis DESC) LIMIT(1)", // TODO MAX(timestampMillis)
                        deviceUuid,
                        sensorTypeUid,
                    ).asFlow()
                        .map { it.list.maxByOrNull { it.timestampMillis }?.toAndroidModel() }
                        .filterNotNull()
                        .distinctUntilChanged()
                        .collect { latestValue ->
                            stateFlow.value = latestValue
                        }
                }
            }
        }
    }

    override fun observeSensorDataInsertions(deviceUuid: String): StateFlow<Long> {
        val stateFlow = MutableStateFlow<Long>(-1) // Initialize with an invalid value (-1 means no data yet)

        CoroutineScope(Dispatchers.IO).launch {
            realm.query<SensorDataRealm>(
                SensorDataRealm::class,
                "deviceUuid == $0",
                deviceUuid,
            )
                .asFlow()
                .filter { it is UpdatedResults } // Only trigger on actual updates (ignore initial results)
                .map { changes ->
                    changes.list.maxByOrNull { it.timestampMillis }?.sensorTypeUid // Get the most recent sensorTypeUid
                }
                .filterNotNull() // Ignore null values
                .distinctUntilChanged() // Avoid emitting the same value multiple times
                .collect { latestSensorTypeUid ->
                    stateFlow.value = latestSensorTypeUid
                }
        }

        return stateFlow
    }
}
