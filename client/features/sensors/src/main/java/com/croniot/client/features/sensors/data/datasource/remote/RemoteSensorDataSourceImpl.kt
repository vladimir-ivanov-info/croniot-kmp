package com.croniot.client.features.sensors.data.datasource.remote

import MqttHandler
import com.croniot.client.core.Global
import com.croniot.client.core.ServerConfig
import com.croniot.client.core.models.SensorData
import com.croniot.client.data.sensors.datasource.RemoteSensorDataSource
import com.croniot.client.data.source.local.RealmRef
import com.croniot.client.features.sensors.data.mqtt.MqttProcessorSensorData
import croniot.models.dto.SensorDataDto
import io.realm.kotlin.Realm
import org.eclipse.paho.client.mqttv3.MqttClient
import java.time.ZonedDateTime

class RemoteSensorDataSourceImpl() : RemoteSensorDataSource {

    private val realm: Realm = RealmRef.realmRef //TODO inject correctly

    override suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit) {

        val clientId = ServerConfig.mqttClientId + Global.generateUniqueString(8)
        val mqttClient = MqttClient(ServerConfig.mqttBrokerUrl, clientId, null)

        val topic = "/server_to_app/$deviceUuid/sensor_data"
        MqttHandler(
            mqttClient,
            MqttProcessorSensorData(onNewSensorDataDto = { newSensorDataDto ->

                val sensorData = newSensorDataDto.toDomain()
                onNewSensorData(sensorData)
                /*CoroutineScope(Dispatchers.IO).launch {
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
                }*/
            }),
            topic,
        )
    }

    override suspend fun stopListening(deviceUuid: String?) {
        //TODO("Not yet implemented")

        //TODO
    }

}

//TODO move to corresponding place
fun SensorDataDto.toDomain() : SensorData {
    return SensorData(
        deviceUuid = this.deviceUuid,
        sensorTypeUid = this.sensorTypeUid,
        value = this.value,
        timeStamp = ZonedDateTime.now()
    )
}