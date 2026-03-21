package com.croniot.client.data.source.sensors

import MqttHandler
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.core.models.SensorData
import com.croniot.client.core.util.StringUtil.generateUniqueString
import com.croniot.client.data.source.local.LocalDatasource
import com.croniot.client.data.source.remote.mappers.toDomain
import com.croniot.client.data.util.TaggingSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import java.util.concurrent.ConcurrentHashMap

class RemoteSensorDataSourceImpl(
    private val appScope: CoroutineScope,
    private val localDatasource: LocalDatasource,
) : RemoteSensorDataSource {

    private val handlersByDevice = ConcurrentHashMap<String, MqttHandler>()

    override suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val clientId = ServerConfig.mqttClientId + generateUniqueString(8)
        val ip = localDatasource.getServerIp().first() ?: "localhost"
        val mqttClient = MqttClient("tcp://${ip}:${ServerConfig.MQTT_PORT}", clientId, null)

        val topic = "/server_to_app/$deviceUuid/sensor_data"
        val handler = MqttHandler(
            mqttClient = mqttClient,
            mqttDataProcessor = MqttProcessorSensorData(onNewSensorDataDto = { newSensorDataDto ->
                val sensorData = newSensorDataDto.toDomain()
                onNewSensorData(sensorData)
            }),
            topic = topic,
            scope = appScope,
            socketFactory = TaggingSocketFactory()
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
