package com.croniot.client.features.sensors.domain.repository

import com.croniot.client.core.models.Device
import com.croniot.client.core.models.SensorData
import com.croniot.client.data.sensors.datasource.LocalSensorDataSource
import com.croniot.client.data.sensors.datasource.RemoteSensorDataSource
import com.croniot.client.data.source.local.RealmRef
import com.croniot.client.domain.repositories.SensorDataRepository
import croniot.models.dto.SensorTypeDto
import io.realm.kotlin.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class SensorDataRepositoryImpl(
    private val remoteSensorDataSource: RemoteSensorDataSource,
    private val localSensorDataSource: LocalSensorDataSource,

) : SensorDataRepository {

    // private val realm: Realm = MyApp.realm
    private val realm: Realm = RealmRef.realmRef

    private val sensorSharedFlows = mutableMapOf<SensorTypeDto, MutableSharedFlow<SensorData>>()
    private val _latestSensorData = mutableMapOf<Pair<String, Long>, MutableStateFlow<SensorData>>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _devicesLatestSensorTimestamp = MutableStateFlow<Map<String, Long>>(emptyMap())
    override val devicesLatestSensorTimestamp: StateFlow<Map<String, Long>> = _devicesLatestSensorTimestamp

    override suspend fun listenToDeviceSensors(device: Device) { // TODO add useCase in the end of fun name
        // TODO data.listenTtoDeviceSensorsUseCase

        /*val clientId = ServerConfig.mqttClientId + Global.generateUniqueString(8)
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
                                timeStampMillis = ZonedDateTime.now().toInstant().toEpochMilli()
                            },
                        )
                    }
                }
            }),
            topic,
        )*/

        remoteSensorDataSource.listenDeviceSensors(
            deviceUuid = device.uuid,
            onNewSensorData = { sensorData ->
                CoroutineScope(Dispatchers.IO).launch {
                    localSensorDataSource.save(sensorData)

                    _devicesLatestSensorTimestamp.update { oldMap ->
                        oldMap + (device.uuid to ZonedDateTime.now().toInstant().toEpochMilli())
                    }
                }
            },
        )

        /*CoroutineScope(Dispatchers.IO).launch {
            remote.incomingData.collect { local.save(it) }
        }*/
    }

    /*override suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int): List<SensorData> {
        return withContext(Dispatchers.IO) {
            realm.query(
                SensorDataRealm::class,
                "deviceUuid == $0 AND sensorTypeUid == $1 SORT(timeStampMillis DESC) LIMIT($elements)",
                deviceUuid,
                sensorTypeUid,
            )
            .find()
            .map { it.toAndroidModel() }
        }
    }*/

    override suspend fun getLatestSensorData(deviceUuid: String, sensorTypeUid: Long, elements: Int) = localSensorDataSource.getLatest(deviceUuid, sensorTypeUid, elements)

// Si

    override /*suspend*/ fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): /*State*/Flow<SensorData> {
        val key = deviceUuid to sensorTypeUid

        /* return*/
       /*localSensorDataSource.observeSensorData(
            deviceUuid = deviceUuid,
            sensorTypeUid = sensorTypeUid
        ).onEach { sensorData ->
            _devicesLatestSensorTimestamp.update { oldMap ->
                oldMap + (deviceUuid to ZonedDateTime.now().toInstant().toEpochMilli())
            }
        }.launchIn(scope = scope)*/

        return localSensorDataSource.observeSensorData(deviceUuid, sensorTypeUid)
        /*return _latestSensorData.getOrPut(key) {
            MutableStateFlow(SensorData(deviceUuid, sensorTypeUid, "0", ZonedDateTime.now())).also { stateFlow -> //TODO the zero
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
        }*/
    }

    /*override fun observeSensorDataInsertions(deviceUuid: String): Flow<Long> {

        return localSensorDataSource.observeSensorData(
            deviceUuid = deviceUuid,
            sensorTypeUid = 1L //TODO
        )*/
/*
        return realm.query(
            SensorDataRealm::class,
            "deviceUuid == $0",
            deviceUuid
        )
            .asFlow()
            .mapNotNull { change ->
                when (change) {
                    is UpdatedResults -> {
                        val idx = change.insertions.maxOrNull() ?: return@mapNotNull null
                        change.list[idx].timeStampMillis  // o .sensorTypeUid si prefieres
                    }
                    else -> null // ignora InitialResults
                }
            }
            .buffer(Channel.CONFLATED)        // evita atasco si llegan muchos
            .flowOn(Dispatchers.Default)      // saca el trabajo del Main
        //.sample(300.milliseconds)       // opcional: suaviza frecuencia
        //.distinctUntilChanged()         // opcional si emites timestamp (ya cambia siempre)


 */
    // }
}
