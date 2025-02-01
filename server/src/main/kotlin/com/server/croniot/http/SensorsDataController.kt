package com.server.croniot.http

import com.server.croniot.mqtt.MqttDataProcessorSensor
import MqttHandler
import com.server.croniot.application.AppComponent
import com.server.croniot.application.DaggerAppComponent
import com.server.croniot.mqtt.MqttController
import com.server.croniot.services.DeviceService
import croniot.messages.MessageSensorData
import croniot.models.dto.SensorDataDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import java.time.ZonedDateTime

class SensorsDataController(
    private val deviceService: DeviceService,
){

    fun start(){
        val devices = deviceService.getAll();

        for(device in devices){
            CoroutineScope(Dispatchers.IO).launch {
                val deviceUuid = device.uuid
                val topic = "/${device.uuid}/sensor_data"

                val mqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))

                val appComponent: AppComponent = DaggerAppComponent.create()

                val sensorsDataController = appComponent.sensorDataController() //TODO pass this            MqttHandler(mqttClient, MqttDataProcessorSensor(deviceUuid, sensorsDataController), topic)
                MqttHandler(mqttClient, MqttDataProcessorSensor(deviceUuid, sensorsDataController), topic)
            }
        }
    }

    fun processSensorData(deviceUuid: String, messageSensorData: MessageSensorData){
        //TODO
        val sensorDataDto = SensorDataDto(deviceUuid, messageSensorData.sensorTypeId, messageSensorData.value, ZonedDateTime.now())

        val device = deviceService.getLazy(deviceUuid)
        device?.let {
            //TODO should check if sensor type actually exists
            //val sensorType = ControllerDb.sensorDao.getLazy(deviceUuid, sensorTypeUid)
            CoroutineScope(Dispatchers.IO).launch {
                MqttController.sendSensorData(sensorDataDto)
            }
        }
    }
}