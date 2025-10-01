package com.croniot.client.core

object ServerConfig {

    val SERVER_ADDRESS_LOCAL = "192.168.50.163"
    var SERVER_ADDRESS_REMOTE = "192.168.50.163"

    var SERVER_ADDRESS = "192.168.50.163"
    var SERVER_PORT = 8090

    // var mqttBrokerUrl = "tcp://51.77.195.204:1883"
    var mqttBrokerUrl = "tcp://192.168.50.163:1883"
    val mqttClientId = "AndroidMQTTClient"
}
