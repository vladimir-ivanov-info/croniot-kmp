package com.croniot.client.core.config

object ServerConfig {
    //var SERVER_PORT = 8090
    var SERVER_PORT = 8443
    const val MQTT_PORT = 1883
    const val mqttClientId = "AndroidMQTTClient"

    const val SERVER_IP_LOCAL = "192.168.50.163"
    const val SERVER_IP_REMOTE = "57.131.29.79"
    //const val SERVER_IP_REMOTE = "192.168.50.163"

    const val DEFAULT_MQTT_HOST = SERVER_IP_REMOTE

    const val CERT_PIN_SHA256 = "sha256/j7Mb7eZ+xUbg6tPxNcWOttDXzoAlppLSMBk8mZI0sak="
}
