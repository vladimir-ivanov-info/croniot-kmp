import croniot.models.MqttDataProcessor

class MqttDataProcessorSensor(val uuid: String, val sensorUid: Long) : MqttDataProcessor {
    override fun process(data: Any) {
        val sensorValue = data as String

        //TODO ControllerDb.getSensorDataDao().insert(uuid, sensorId, sensorValue)
    }

    override fun getTopic(): String {
        return "esp32id_outcoming/sensor_data/2" //Should name this class "DataProcessorClientOutcoming"
    }
}