import croniot.models.MqttDataProcessor
import org.eclipse.paho.client.mqttv3.*

class MqttHandler(private val mqttClient: MqttClient, mqttDataProcessor: MqttDataProcessor, private val topic: String) {

    // TODO if device has no sensors, don't subscribe to topic.
    init {
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true

        mqttClient.connect(options)

        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                println("Connection lost: " + topic + " ${cause?.message}")
                println("Reconnecting...")
                mqttClient.connect(options)
            }

            override fun messageArrived(topic: String, message: MqttMessage?) {
                val payload = message?.payload
                if (payload != null) {
                    val value = String(payload)
                    // println("Received message on topic $topic: $value")
                    mqttDataProcessor.process(topic, value)
                } else {
                    println("Received message on topic $topic with null payload.")
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // Not used in this example
            }
        })
        println("MQTT Subscribed to " + topic)
        mqttClient.subscribe(topic, 2) // QoS 2 for subscribing
    }

    fun disconnect() {
        runCatching { mqttClient.unsubscribe(topic) }
        runCatching { mqttClient.disconnect() }
        runCatching { mqttClient.close() }
    }
}
