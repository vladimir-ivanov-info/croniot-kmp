package croniot.models

interface MqttDataProcessor {
    fun process(data: Any)

    fun getTopic() : String
}