package com.croniot.android.data

import com.croniot.android.presentation.devices.DevicesViewModel
import croniot.models.MqttDataProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MqttProcessorSensorData(val clientUuid: String, val sensorUuid: String) : MqttDataProcessor, KoinComponent {

    private val viewModelSensorData: com.croniot.android.ViewModelSensorData = get()
    private val devicesViewModel: DevicesViewModel = get()

    override fun getTopic(): String {
        TODO("Not yet implemented")
    }

    override fun process(data: Any) {
        CoroutineScope(Dispatchers.IO).launch {
            val sensorValue = data as String
            println(sensorValue)
            viewModelSensorData.updateSensorData(clientUuid, sensorUuid, sensorValue)
            devicesViewModel.updateDeviceOnlineStatus(clientUuid)
        }
    }
}