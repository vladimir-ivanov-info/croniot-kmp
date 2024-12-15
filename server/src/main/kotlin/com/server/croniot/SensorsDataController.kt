package com.server.croniot

import MqttController
import MqttDataProcessorSensor
import MqttHandler
import com.croniot.server.db.controllers.ControllerDb
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
            for(sensorType in device.sensorTypes){
                val deviceUuid = device.uuid
                val topic = "/${device.uuid}/sensor_data/${sensorType.uid}"

                val mqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))
                var mqttHandler = MqttHandler(mqttClient, MqttDataProcessorSensor(deviceUuid, sensorType.uid), topic)
            }
        }
    }

    fun processSensorData(deviceUuid: String, sensorTypeUid: Long, sensorValue: String){
        val sensorDataDto = SensorDataDto(deviceUuid, sensorTypeUid, sensorValue, ZonedDateTime.now())

        val device = ControllerDb.deviceDao.getLazy(deviceUuid)
        device?.let {
            val sensorType = ControllerDb.sensorDao.getLazy(deviceUuid, sensorTypeUid)

            sensorType?.let {
                val sensorData = SensorData(device, sensorType, sensorValue, ZonedDateTime.now())
                ControllerDb.sensorDataDao.insert(sensorData)

                CoroutineScope(Dispatchers.IO).launch {
                    MqttController.sendSensorData(sensorDataDto)
                }
            }
        }
    }
}