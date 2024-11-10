package com.croniot.android.presentation.device.sensors

import MqttHandler
import MqttProcessorMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.Global
import com.croniot.android.GlobalViewModel
import com.croniot.android.data.MqttProcessorSensorData
import com.croniot.android.presentation.login.LoginController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import croniot.models.SensorData
import croniot.models.dto.DeviceDto
import croniot.models.dto.SensorDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ViewModelSensors() : ViewModel(), KoinComponent {

    private var _listeningToClientSensors = false

    private val _mapStateFlow = MutableStateFlow<Map<SensorDto, MutableStateFlow<SensorData>>>(emptyMap())
    val mapStateFlow: StateFlow<Map<SensorDto, MutableStateFlow<SensorData>>> = _mapStateFlow

    private var _gps : MutableStateFlow<String> = MutableStateFlow("")
    val gps : MutableStateFlow<String> get() = _gps

    val mqttClients : MutableList<MqttClient> = mutableListOf()

    private val globalViewModel: GlobalViewModel = get()

    fun uninit(){
        viewModelScope.launch{
            for(mqttClient in mqttClients){
                mqttClient.disconnect()
            }
            mqttClients.clear()

            //_map.values.clear()
            _mapStateFlow.value = emptyMap()
            _gps.value = ""

            _listeningToClientSensors = false
        }
    }

    init {
        listenToClientSensorsIfNeeded()
    }

    suspend fun isAccountInitialized() : Boolean {
        return withContext(Dispatchers.Main) {
            globalViewModel.account.value != null
        }
    }

    fun listenToClientSensorsIfNeeded(/*devices: List<DeviceDto>*/){

        // Global.account.devices.toList()
        val account = globalViewModel.account.value
        if(account != null){

            val devices = account.devices

            viewModelScope.launch(Dispatchers.IO) { //Note: Dispatcher very important
                if(mqttClients.isEmpty()){
                    _listeningToClientSensors = true

                    for(device in devices){
                        val clientUuid = device.uuid

                        for(sensor in device.sensors){
                            val topic = "/${device.uuid}/sensor_data/${sensor.uid}"
                            val mqttClient = MqttClient(
                                Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(
                                    8
                                ), null) //TODO (Ver luego) sin null da: org.eclipse.paho.client.mqttv3.MqttPersistenceException

                            MqttHandler(mqttClient, MqttProcessorSensorData(clientUuid, sensor.uid.toString()), topic)

                            mqttClients.add(mqttClient)

                            val value = SensorData(clientUuid, sensor.uid.toString(), "empty_value", "todo_last_date") //TODO empty_value is a constant

                            //_map[sensor] = MutableStateFlow(value)
                            _mapStateFlow.value = _mapStateFlow.value.toMutableMap().apply {
                                this[sensor] = MutableStateFlow(value)
                            }
                        }
                    }
                }
            }
        } else {

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
            // Create a new mutable map from the current value
            val updatedMap = _mapStateFlow.value.toMutableMap()

            // Iterate over the map and find the matching entry to update
            updatedMap.forEach { (sensor, flow) ->
                if (flow.value.clientUuid == deviceUuid && flow.value.sensorId == sensorId) {
                    // Emit new data to the MutableStateFlow for the matching entry
                    flow.emit(SensorData(deviceUuid, sensorId, newSensorData, "dateTimeTODO"))
                }
            }

            // Update _mapStateFlow with the modified map
            _mapStateFlow.value = updatedMap
        }
    }

}