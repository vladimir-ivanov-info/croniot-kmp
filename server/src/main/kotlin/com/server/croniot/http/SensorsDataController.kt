package com.server.croniot.http

import Global
import MqttHandler
import com.server.croniot.application.ApplicationScope
import com.server.croniot.mqtt.MqttController
import com.server.croniot.mqtt.MqttDataProcessorSensor
import com.server.croniot.services.DeviceService
import croniot.messages.MessageSensorData
import croniot.models.dto.SensorDataDto
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.time.ZonedDateTime

class SensorsDataController(
    private val deviceService: DeviceService,
    private val applicationScope: ApplicationScope,
) {

    fun start() {
        val devices = deviceService.getAll()

        for (device in devices) {
            applicationScope.launch {
                val deviceUuid = device.uuid
                val topic = "/${device.uuid}/sensor_data"

                val mqttClient = MqttClient(
                    Global.secrets.mqttBrokerUrl,
                    Global.secrets.mqttClientId + Global.generateUniqueString(8),
                    MemoryPersistence()
                )

                MqttHandler(
                    mqttClient,
                    MqttDataProcessorSensor(deviceUuid, this@SensorsDataController),
                    topic,
                    applicationScope
                )
            }
        }
    }

    fun processSensorData(deviceUuid: String, messageSensorData: MessageSensorData) {
        val sensorDataDto = SensorDataDto(
            deviceUuid,
            messageSensorData.sensorTypeId,
            messageSensorData.value,
            ZonedDateTime.now()
        )

        val device = deviceService.getLazy(deviceUuid)
        device?.let {
            applicationScope.launch {
                MqttController.sendSensorData(sensorDataDto)
            }
        }
    }
}
