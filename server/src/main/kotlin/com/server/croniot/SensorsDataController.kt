package com.server.croniot

import MqttController
import MqttDataProcessorSensor
import MqttHandler
import com.croniot.server.db.controllers.ControllerDb
import croniot.messages.MessageSensorData
import croniot.models.SensorData
import croniot.models.dto.SensorDataDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import java.time.ZonedDateTime

object SensorsDataController {
    init {
        val devices = ControllerDb.deviceDao.getAll();

        for(device in devices){

            val deviceUuid = device.uuid
            val topic = "/${device.uuid}/sensor_data"

            val mqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))
            MqttHandler(mqttClient, MqttDataProcessorSensor(deviceUuid), topic)
        }
    }

    fun processSensorData(deviceUuid: String, messageSensorData: MessageSensorData, /*sensorTypeUid: Long, sensorValue: String*/){

        val sensorDataDto = SensorDataDto(deviceUuid, messageSensorData.sensorTypeId, messageSensorData.value, ZonedDateTime.now())

        val device = ControllerDb.deviceDao.getLazy(deviceUuid)
        device?.let {
            //TODO should check if sensor type actually exists
            //val sensorType = ControllerDb.sensorDao.getLazy(deviceUuid, sensorTypeUid)
            CoroutineScope(Dispatchers.IO).launch {
                MqttController.sendSensorData(sensorDataDto)
            }
        }
    }
}