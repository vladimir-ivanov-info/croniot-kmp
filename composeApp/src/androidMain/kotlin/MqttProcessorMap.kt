import com.croniot.android.presentation.device.sensors.ViewModelSensors
import croniot.models.MqttDataProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.koin.core.component.KoinComponent
import org.koin.core.component.get

//TODO experimental
class MqttProcessorMap() : MqttDataProcessor, KoinComponent {

    private val viewModelSensors: ViewModelSensors = get()

    override fun getTopic(): String {
        TODO("Not yet implemented")
    }

    override fun process(data: Any) {
        CoroutineScope(Dispatchers.IO).launch {
            val mapValue = data as String
            println(mapValue)
            viewModelSensors.updateMap(mapValue)
        }
    }
}