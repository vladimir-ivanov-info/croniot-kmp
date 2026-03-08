package com.croniot.client.data.source.sensors

import MqttHandler
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.core.models.SensorData
import com.croniot.client.core.util.StringUtil.generateUniqueString
import com.croniot.client.data.source.remote.mappers.toDomain
import org.eclipse.paho.client.mqttv3.MqttClient
import java.util.concurrent.ConcurrentHashMap

class RemoteSensorDataSourceImpl : RemoteSensorDataSource {

    private val handlersByDevice = ConcurrentHashMap<String, MqttHandler>()

    override suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit,
    ) {
        val clientId = ServerConfig.mqttClientId + generateUniqueString(8)
        val mqttClient = MqttClient(ServerConfig.mqttBrokerUrl, clientId, null)

        val topic = "/server_to_app/$deviceUuid/sensor_data"
        val handler = MqttHandler(
            mqttClient,
            MqttProcessorSensorData(onNewSensorDataDto = { newSensorDataDto ->
                val sensorData = newSensorDataDto.toDomain()
                onNewSensorData(sensorData)
            }),
            topic
        )
        handlersByDevice[deviceUuid] = handler
    }

    override suspend fun stopListening(deviceUuid: String?) {
        if (deviceUuid != null) {
            handlersByDevice.remove(deviceUuid)?.disconnect()
        } else {
            handlersByDevice.values.forEach { it.disconnect() }
            handlersByDevice.clear()
        }
    }
}
