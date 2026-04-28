package com.croniot.client.data.source.sensors

import MqttHandler
import Outcome
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.SensorData
import com.croniot.client.core.util.StringUtil.generateUniqueString
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import com.croniot.client.data.source.remote.mappers.toDomain
import croniot.models.MqttTopics
import com.croniot.client.data.util.TaggingSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttException
import java.util.concurrent.ConcurrentHashMap

class RemoteSensorDataSourceImpl(
    private val appScope: CoroutineScope,
    private val localDatasource: ServerConfigLocalDatasource,
) : RemoteSensorDataSource {

    private val handlersByDevice = ConcurrentHashMap<String, MqttHandler>()

    override suspend fun listenDeviceSensors(
        deviceUuid: String,
    ): Outcome<Flow<SensorData>, ConnectionError> = withContext(Dispatchers.IO) {
        val clientId = ServerConfig.mqttClientId + generateUniqueString(8)
        val ip = localDatasource.getServerIp().first() ?: ServerConfig.DEFAULT_MQTT_HOST
        val brokerUrl = "tcp://${ip}:${ServerConfig.MQTT_PORT}"

        try {
            val mqttClient = MqttClient(brokerUrl, clientId, null)
            val topic = MqttTopics.sensorData(deviceUuid)
            val sensorDataFlow = MutableSharedFlow<SensorData>(extraBufferCapacity = 64)
            val handler = MqttHandler(
                mqttClient = mqttClient,
                mqttDataProcessor = MqttProcessorSensorData(onNewSensorDataDto = { newSensorDataDto ->
                    sensorDataFlow.tryEmit(newSensorDataDto.toDomain())
                }),
                topic = topic,
                scope = appScope,
                socketFactory = TaggingSocketFactory()
            )
            handlersByDevice[deviceUuid] = handler
            Outcome.Ok(sensorDataFlow.asSharedFlow())
        } catch (e: MqttException) {
            Outcome.Err(ConnectionError.MqttBrokerUnreachable(host = brokerUrl, cause = e.message))
        } catch (e: Exception) {
            Outcome.Err(ConnectionError.Unknown)
        }
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
