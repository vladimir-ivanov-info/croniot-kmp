package croniot.models

interface MqttDataProcessor {
    fun process(data: Any)
}
