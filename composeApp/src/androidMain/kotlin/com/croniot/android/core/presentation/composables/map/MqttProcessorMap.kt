package com.croniot.android.core.presentation.composables.map

import com.croniot.client.features.sensors.presentation.SensorsViewModel
import croniot.models.MqttDataProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

// TODO experimental
class MqttProcessorMap() : MqttDataProcessor, KoinComponent {

    private val sensorsViewModel: SensorsViewModel = get()

    override fun process(data: Any) {
        CoroutineScope(Dispatchers.IO).launch {
            val mapValue = data as String
            println(mapValue)
            // TODO viewModelSensors.updateMap(mapValue)
        }
    }
}
