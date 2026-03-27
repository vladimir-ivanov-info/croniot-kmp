import croniot.models.MqttDataProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import javax.net.SocketFactory

class MqttHandler(
    private val mqttClient: MqttClient,
    private val mqttDataProcessor: MqttDataProcessor,
    private val topic: String,
    private val scope: CoroutineScope,
    private val socketFactory: SocketFactory? = null
) {

    companion object {
        const val DEFAULT_QOS = 2
    }

    // TODO if device has no sensors, don't subscribe to topic.
    init {
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        socketFactory?.let {
            options.socketFactory = it
        }

        mqttClient.connect(options)

        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                if (reconnect) {
                    println("[RTT] MQTT reconnected: topic=$topic, re-subscribing...")
                    runCatching { mqttClient.subscribe(topic, DEFAULT_QOS) }
                }
            }

            override fun connectionLost(cause: Throwable?) {
                println("[RTT] MQTT connectionLost: topic=$topic cause=${cause?.message}")
            }

            override fun messageArrived(topic: String, message: MqttMessage?) {
                val payload = message?.payload
                if (payload != null) {
                    val value = String(payload)
                    scope.launch {
                        mqttDataProcessor.process(topic, value)
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
        println("MQTT Subscribed to " + topic)
        mqttClient.subscribe(topic, DEFAULT_QOS)
    }

    fun disconnect() {
        runCatching { mqttClient.unsubscribe(topic) }
        runCatching { mqttClient.disconnect() }
        runCatching { mqttClient.close() }
    }
}
