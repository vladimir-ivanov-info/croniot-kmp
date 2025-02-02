package com.croniot.android.core.data.source.repository

import MqttHandler
import com.croniot.android.app.Global
import com.croniot.android.app.MyApp
import com.croniot.android.core.data.entities.SensorDataRealm
import com.croniot.android.features.device.features.sensors.data.processor.MqttProcessorSensorData
import com.croniot.android.features.device.features.sensors.presentation.toSensorDataDto
import croniot.models.dto.DeviceDto
import croniot.models.dto.SensorDataDto
import croniot.models.dto.SensorTypeDto
import io.realm.kotlin.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import java.time.ZonedDateTime

class SensorDataRepositoryImpl() : SensorDataRepository {

    private val realm: Realm = MyApp.realm

    private val sensorSharedFlows = mutableMapOf<SensorTypeDto, MutableSharedFlow<SensorDataDto>>()
    private val _latestSensorData = mutableMapOf<Pair<String, Long>, MutableStateFlow<SensorDataDto>>()

    override suspend fun listenToDeviceSensors(device: DeviceDto) {
        val clientId = Global.mqttClientId + Global.generateUniqueString(8)
        val mqttClient = MqttClient(Global.mqttBrokerUrl, clientId, null)

        val topic = "/server_to_app/${device.uuid}/sensor_data"
        MqttHandler(
            mqttClient,
            MqttProcessorSensorData(onNewData = { newSensorData ->
                CoroutineScope(Dispatchers.IO).launch {
                    realm.writeBlocking {
                        copyToRealm(
                            SensorDataRealm().apply {
                                deviceUuid = newSensorData.deviceUuid // Ensure newSensorData carries the deviceUuid
                                sensorTypeUid = newSensorData.sensorTypeUid
                                value = newSensorData.value
                                timestamp = ZonedDateTime.now().toString()
                            },
                        )
                    }
                }
            }),
            topic,
        )
    }

    override fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): StateFlow<SensorDataDto> {
        val key = deviceUuid to sensorTypeUid
        return _latestSensorData.getOrPut(key) {
            MutableStateFlow(SensorDataDto(deviceUuid, sensorTypeUid, "0", ZonedDateTime.now())).also { stateFlow ->
                CoroutineScope(Dispatchers.IO).launch {
                    realm.query(
                        SensorDataRealm::class,
                        "deviceUuid == $0 AND sensorTypeUid == $1",
                        deviceUuid,
                        sensorTypeUid,
                    ).asFlow()
                        .map { it.list.maxByOrNull { it.timestamp }?.toSensorDataDto() }
                        .filterNotNull()
                        .distinctUntilChanged()
                        .collect { latestValue ->
                            stateFlow.value = latestValue
                        }
                }
            }
        }
    }

    override suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int): List<SensorDataDto> {
        return withContext(Dispatchers.IO) {
            realm.query(
                SensorDataRealm::class,
                "deviceUuid == $0 AND sensorTypeUid == $1 SORT(timestamp DESC) LIMIT($elements)",
                deviceUuid,
                sensorTypeUid,
            )
                .find()
                .map { it.toSensorDataDto() }
        }
    }
}
