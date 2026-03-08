package com.croniot.client.core.config

object ServerConfig {
    const val SERVER_ADDRESS_LOCAL = "192.168.50.163"
    const val SERVER_ADDRESS_REMOTE = "192.168.50.163"

    var SERVER_ADDRESS = "192.168.50.163"
    var SERVER_PORT = 8090

    var mqttBrokerUrl = "tcp://192.168.50.163:1883"
    const val mqttClientId = "AndroidMQTTClient"
}
