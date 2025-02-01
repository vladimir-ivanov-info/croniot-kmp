package com.croniot.android.features.device.features.sensors.presentation

import androidx.lifecycle.ViewModel
import com.croniot.android.core.data.source.repository.SensorDataRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ViewModelSensors() : ViewModel(), KoinComponent {

    private val sensorDataRepositoryImpl: SensorDataRepository = get()
    val sensorDataStateFlow = sensorDataRepositoryImpl.getStateFlow()
}

// TODO experimental
//    fun listenToMapUpdates(){
//
//        Global.mqttBrokerUrl = "tcp://51.77.195.204:1883"
//
//        val topic = "/gps"
//        try{
//            var mqttClient = MqttClient(
//                Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(
//                    8
//                ), null) //TODO (Ver luego) sin null da: org.eclipse.paho.client.mqttv3.MqttPersistenceException
//
//            MqttHandler(mqttClient, MqttProcessorMap(), topic)
//
//        } catch (e: Exception){
//            println("error")
//        }
//    }
//
//    //TODO experimental
//    fun updateMap(value: String){
//        viewModelScope.launch {
//            _gps.emit(value)
//        }
//    }
