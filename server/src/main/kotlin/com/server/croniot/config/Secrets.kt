package com.server.croniot.config

data class Secrets(
    val mqttBrokerUrl: String,
    val mqttClientId: String,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
)
