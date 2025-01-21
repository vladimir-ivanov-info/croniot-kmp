import com.google.gson.GsonBuilder
import com.server.croniot.SensorsDataController
import croniot.messages.MessageSensorData
import croniot.models.MqttDataProcessor
import java.time.ZonedDateTime

class MqttDataProcessorSensor(val deviceUuid: String/*, val sensorTypeUid: Long*/) : MqttDataProcessor {

    val gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeSerializer())
        .create()

    override fun process(data: Any) {
        val messageString = data as String
//TODO handle error in fromJson
        val messageSensorData = gson.fromJson(messageString, MessageSensorData::class.java)

       SensorsDataController.processSensorData(deviceUuid, messageSensorData)
    }

    override fun getTopic(): String {
        return "esp32id_outcoming/sensor_data/2" //Should name this class "DataProcessorClientOutcoming"
    }
}