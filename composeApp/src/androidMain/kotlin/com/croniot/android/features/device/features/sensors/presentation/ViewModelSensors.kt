package com.croniot.android.features.device.features.sensors.presentation

import MqttHandler
import com.croniot.android.core.presentation.composables.map.MqttProcessorMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.app.Global
import com.croniot.android.app.GlobalViewModel
import com.croniot.android.features.device.features.sensors.data.processor.MqttProcessorSensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import croniot.models.dto.SensorDataDto
import croniot.models.dto.SensorTypeDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.time.ZonedDateTime

class ViewModelSensors() : ViewModel(), KoinComponent {

    private var _listeningToClientSensors = false

    private val _mapStateFlow = MutableStateFlow<Map<SensorTypeDto, MutableStateFlow<SensorDataDto>>>(emptyMap())
    val mapStateFlow: StateFlow<Map<SensorTypeDto, MutableStateFlow<SensorDataDto>>> = _mapStateFlow

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

    fun listenToClientSensorsIfNeeded(){

        val account = globalViewModel.account.value
        account?.let{
            val devices = it.devices

            viewModelScope.launch(Dispatchers.IO) { //Note: Dispatcher very important
                if(mqttClients.isEmpty()){
                    _listeningToClientSensors = true

                    for(device in devices){
                        val deviceUuid = device.uuid

                        for(sensor in device.sensors){
                            val topic = "/server_to_app/${device.uuid}/sensor_data/${sensor.uid}"
                            val mqttClient = MqttClient(
                                Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(
                                    8
                                ), null) //TODO (Ver luego) sin null da: org.eclipse.paho.client.mqttv3.MqttPersistenceException

                            MqttHandler(mqttClient, MqttProcessorSensorData(deviceUuid, sensor.uid.toString()), topic)

                            mqttClients.add(mqttClient)

                            val value = SensorDataDto(deviceUuid, sensor.uid, "empty_value", ZonedDateTime.now()) //TODO empty_value is a constant

                            _mapStateFlow.value = _mapStateFlow.value.toMutableMap().apply {
                                this[sensor] = MutableStateFlow(value)
                            }
                        }
                    }
                }
            }
        } /*else {

        }*/
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
                if (flow.value.deviceUuid == deviceUuid && flow.value.sensorTypeUid == sensorId.toLong()) {
                    // Emit new data to the MutableStateFlow for the matching entry
                    flow.emit(SensorDataDto(deviceUuid, sensorId.toLong(), newSensorData, ZonedDateTime.now()))
                }
            }

            // Update _mapStateFlow with the modified map
            _mapStateFlow.value = updatedMap
        }
    }

}