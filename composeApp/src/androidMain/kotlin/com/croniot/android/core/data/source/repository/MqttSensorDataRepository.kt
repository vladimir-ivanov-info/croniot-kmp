package com.croniot.android.core.data.source.repository

import MqttHandler
import com.croniot.android.app.Global
import com.croniot.android.features.device.features.sensors.data.processor.MqttProcessorSensorData
import croniot.models.dto.SensorDataDto
import croniot.models.dto.SensorTypeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.eclipse.paho.client.mqttv3.MqttClient
import java.time.ZonedDateTime

class MqttSensorDataRepository() : SensorDataRepository {

    private var _sensorDataStateFlow = MutableStateFlow<Map<SensorTypeDto, MutableStateFlow<SensorDataDto>>>(emptyMap())
    val sensorDataStateFlow : StateFlow<Map<SensorTypeDto, MutableStateFlow<SensorDataDto>>> get() = _sensorDataStateFlow

    override suspend fun listenToSensor(deviceUuid: String, sensorType: SensorTypeDto) {
        val clientId = Global.mqttClientId + Global.generateUniqueString(8);
        val mqttClient = MqttClient(Global.mqttBrokerUrl, clientId, null)

        val initialSensorData = SensorDataDto(
            deviceUuid = deviceUuid,
            sensorTypeUid = sensorType.uid,
            value = "empty_value",
            timestamp = ZonedDateTime.now()
        )

        addOrUpdateSensor(sensorType, initialSensorData)

        val topic = "/server_to_app/${deviceUuid}/sensor_data/${sensorType.uid}"
        MqttHandler(mqttClient, MqttProcessorSensorData(deviceUuid, sensorType.uid.toString(), onNewData = { newSensorData ->
            //TODO

        _sensorDataStateFlow.value[sensorType]?.let { sensorDataFlow ->
            sensorDataFlow.update { currentData ->
                currentData.copy(
                    value = newSensorData,
                    timestamp = ZonedDateTime.now() // Update the timestamp to the current time
                )
            }
        }


        }), topic)
    }

    private fun addOrUpdateSensor(
        sensorType: SensorTypeDto,
        sensorData: SensorDataDto
    ) {
        val currentMap = sensorDataStateFlow.value.toMutableMap()
        currentMap[sensorType] = MutableStateFlow(sensorData)
        _sensorDataStateFlow.value = currentMap
    }
}

