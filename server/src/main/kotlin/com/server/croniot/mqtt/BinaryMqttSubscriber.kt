package com.server.croniot.mqtt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

// Binary-safe counterpart to the shared MqttHandler
// (shared/src/commonMain/kotlin/MqttHandler.kt), which converts every
// payload to `String(payload)` before it ever reaches a processor - fine
// for the JSON messages every other MqttHandler use in this codebase
// carries, lossy/corrupting for the CBOR-encoded device_log/device_event
// batches this subscriber exists to receive. Kept server-only (a new
// class here, not a change to the shared one the Android client also
// depends on) since only the server ever needs binary MQTT payloads
// today - see MqttController.initDeviceLogController().
//
// Also supports wildcard topic filters directly (e.g.
// "/iot_to_server/logs/+") - messageArrived's own `topic` argument is
// always the concrete resolved topic, never the pattern, so `onMessage`
// receives the real per-device topic to parse a device UUID out of.
class BinaryMqttSubscriber(
    private val mqttClient: MqttClient,
    private val topicFilter: String,
    private val qos: Int,
    private val scope: CoroutineScope,
    private val onMessage: suspend (topic: String, payload: ByteArray) -> Unit,
) {
    init {
        val options = MqttConnectOptions().apply { isAutomaticReconnect = true }
        mqttClient.connect(options)

        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                if (reconnect) {
                    runCatching { mqttClient.subscribe(topicFilter, qos) }
                }
            }

            override fun connectionLost(cause: Throwable?) {
                println("[BinaryMqttSubscriber] connectionLost topic=$topicFilter cause=${cause?.message}")
            }

            override fun messageArrived(topic: String, message: MqttMessage?) {
                val payload = message?.payload
                if (payload != null) {
                    scope.launch { onMessage(topic, payload) }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        mqttClient.subscribe(topicFilter, qos)
    }
}
