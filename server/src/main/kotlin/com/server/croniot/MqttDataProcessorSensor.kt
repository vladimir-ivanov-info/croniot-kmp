import com.croniot.server.db.controllers.ControllerDb
import com.server.croniot.SensorsDataController
import croniot.models.MqttDataProcessor

class MqttDataProcessorSensor(val deviceUuid: String, val sensorTypeUid: Long) : MqttDataProcessor {
    override fun process(data: Any) {
        val sensorValue = data as String

       SensorsDataController.processSensorData(deviceUuid, sensorTypeUid, sensorValue)
    }

    override fun getTopic(): String {
        return "esp32id_outcoming/sensor_data/2" //Should name this class "DataProcessorClientOutcoming"
    }
}