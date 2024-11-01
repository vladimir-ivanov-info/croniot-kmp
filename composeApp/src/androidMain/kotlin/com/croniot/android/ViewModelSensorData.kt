package com.croniot.android

import MqttHandler
import MqttProcessorMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.data.MqttProcessorSensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import croniot.models.SensorData
import croniot.models.dto.DeviceDto
import croniot.models.dto.SensorDto
import kotlinx.coroutines.Dispatchers
import org.eclipse.paho.client.mqttv3.MqttClient
import org.koin.core.component.KoinComponent

class ViewModelSensorData() : ViewModel(), KoinComponent {

    private var _listeningToClientSensors = false

    private var _map : MutableMap<SensorDto, MutableStateFlow<SensorData>> = mutableMapOf()
    val map : MutableMap<SensorDto, MutableStateFlow<SensorData>> get() = _map

    private var _gps : MutableStateFlow<String> = MutableStateFlow("")
    val gps : MutableStateFlow<String> get() = _gps

    fun listenToClientSensorsIfNeeded(devices: List<DeviceDto>){
        viewModelScope.launch(Dispatchers.IO) { //Note: Dispatcher very important
            if(!_listeningToClientSensors){
                _listeningToClientSensors = true

                for(device in devices){
                    val clientUuid = device.uuid

                    for(sensor in device.sensors){
                        val topic = "/${device.uuid}/sensor_data/${sensor.uid}"
                        var mqttClient = MqttClient(
                            Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(
                                8
                            ), null) //TODO (Ver luego) sin null da: org.eclipse.paho.client.mqttv3.MqttPersistenceException

                        MqttHandler(mqttClient, MqttProcessorSensorData(clientUuid, sensor.uid.toString()), topic)

                        var value = SensorData(clientUuid, sensor.uid.toString(), "empty_value", "todo_last_date") //TODO empty_value is a constant

                        _map[sensor] = MutableStateFlow(value)
                    }
                }
            }
        }
    }

    //TODO experimental
    fun listenToMapUpdates(){

        Global.mqttBrokerUrl = "tcp://51.77.195.204:1883"

        val topic = "/gps"

        try{
            var mqttClient = MqttClient(
                Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(
                    8
                ), null) //TODO (Ver luego) sin null da: org.eclipse.paho.client.mqttv3.MqttPersistenceException

            MqttHandler(mqttClient, MqttProcessorMap(), topic)

        } catch (e: Exception){
            println("error")
        }
    }

    //TODO experimental
    fun updateMap(value: String){
        viewModelScope.launch {
            _gps.emit(value)
        }
    }

    fun updateSensorData(deviceUuid: String, sensorId: String, newSensorData: String) {
        viewModelScope.launch {
            for(flow in map){
                if(flow.value.value.clientUuid == deviceUuid && flow.value.value.sensorId == sensorId){
                    flow.value.emit(SensorData(deviceUuid, sensorId, newSensorData, "dateTimeTODO"))
                }
            }
        }
    }
}