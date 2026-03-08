package croniot.models

interface MqttDataProcessor {
    //fun process(data: Any)

    fun process(topic: String, data: Any)
}
