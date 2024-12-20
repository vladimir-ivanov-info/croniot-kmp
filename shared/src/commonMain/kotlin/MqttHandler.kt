import croniot.messages.MessageFactory
import croniot.messages.MessageSensorData
import croniot.models.MqttDataProcessor
import org.eclipse.paho.client.mqttv3.*


class MqttHandler(mqttClient: MqttClient, mqttDataProcessor: MqttDataProcessor, topic: String) {

    //private val scope = CoroutineScope(Dispatchers.IO) // Coroutine scope for handling message processing


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

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message?.payload
                if (payload != null) {
                    val value = String(payload)
                    //println("Received message on topic $topic: $value")
                    mqttDataProcessor.process(value) //datawrapper nees to be generic too. Better use map key-value I think

                    //scope.launch {
                      //  mqttDataProcessor.process(value)
                   // }

                } else {
                    println("Received message on topic $topic with null payload.")
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // Not used in this example
            }
        })
        println("Server subscribed to " + topic)
        //mqttClient.subscribe(topic)
        mqttClient.subscribe(topic, 2) // QoS 2 for subscribing
    }


//        fun disconnect() {
//            mqttClient.disconnect()
//        }


    fun processMessage(message: String){
        val messageSenosorData = MessageFactory.fromJson<MessageSensorData>(message)

        val id = messageSenosorData.id
        val value = messageSenosorData.value
    }

}