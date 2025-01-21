package com.croniot.android.core.data.source.repository

import MqttHandler
import com.croniot.android.app.Global
import com.croniot.android.features.device.features.sensors.data.processor.MqttProcessorSensorData
import croniot.models.dto.DeviceDto
import croniot.models.dto.SensorDataDto
import croniot.models.dto.SensorTypeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.eclipse.paho.client.mqttv3.MqttClient
import java.time.ZonedDateTime

class SensorDataRepositoryImpl() : SensorDataRepository {

    private var _sensorDataStateFlow = MutableStateFlow<Map<SensorTypeDto, MutableStateFlow<SensorDataDto>>>(emptyMap())
    val sensorDataStateFlow : StateFlow<Map<SensorTypeDto, MutableStateFlow<SensorDataDto>>> get() = _sensorDataStateFlow

    override fun getStateFlow(): StateFlow<Map<SensorTypeDto, MutableStateFlow<SensorDataDto>>> {
       return sensorDataStateFlow
    }

    override suspend fun listenToDeviceSensors(device: DeviceDto) {

        val clientId = Global.mqttClientId + Global.generateUniqueString(8);
        val mqttClient = MqttClient(Global.mqttBrokerUrl, clientId, null)

        //get sensors and give them initial value
        for(sensor in device.sensors){
            val initialSensorData = SensorDataDto(
                deviceUuid = device.uuid,
                sensorTypeUid = sensor.uid,
                value = "empty_value",
                timestamp = ZonedDateTime.now()
            )
            addOrUpdateSensor(sensor, initialSensorData)
        }

        val topic = "/server_to_app/${device.uuid}/sensor_data"
        MqttHandler(mqttClient, MqttProcessorSensorData(onNewData = { newSensorData ->

            //find sensor //TODO optimize searching
            val targetUid = newSensorData.sensorTypeUid // Replace with your desired UID
            val result = _sensorDataStateFlow.value.entries
                .firstOrNull { (_, stateFlow) -> stateFlow.value.sensorTypeUid == targetUid }
                ?.value

            if (result != null) {
                println("Found MutableStateFlow with UID $targetUid: $result")

                result.update { currentData ->
                    currentData.copy(
                        value = newSensorData.value,
                        timestamp = ZonedDateTime.now() // Update the timestamp to the current time
                    )
                }

            } else {
                println("No MutableStateFlow found with UID $targetUid")
            }
        }), topic)
    }

    private fun addOrUpdateSensor(sensorType: SensorTypeDto, sensorData: SensorDataDto) {
        val currentMap = sensorDataStateFlow.value.toMutableMap()
        currentMap[sensorType] = MutableStateFlow(sensorData)
        _sensorDataStateFlow.value = currentMap
    }
}