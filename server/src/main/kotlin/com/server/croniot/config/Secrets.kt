package com.server.croniot.config

import java.io.File
import java.util.Properties

data class Secrets(
    val mqttBrokerUrl: String,
    val mqttClientId: String,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val keystoreUser: String,
    val keystorePassword: String,
) {
    companion object {
        private val localProperties: Properties by lazy {
            Properties().apply {
                val candidates = listOf(
                    File("local.properties"),
                    File("../local.properties"),
                )
                candidates.firstOrNull { it.exists() }?.inputStream()?.use { load(it) }
            }
        }

        fun fromEnvironment(): Secrets {
            return Secrets(
                mqttBrokerUrl = resolve("CRONIOT_MQTT_BROKER_URL"),
                mqttClientId = resolve("CRONIOT_MQTT_CLIENT_ID"),
                databaseUrl = resolve("CRONIOT_DB_URL"),
                databaseUser = resolve("CRONIOT_DB_USER"),
                databasePassword = resolve("CRONIOT_DB_PASSWORD"),
                keystoreUser = resolve("CRONIOT_KEYSTORE_USER"),
                keystorePassword = resolve("CRONIOT_KEYSTORE_PASSWORD"),
            )
        }

        private fun resolve(name: String): String {
            return System.getenv(name)
                ?: localProperties.getProperty(name)
                ?: ""
        }
    }
}
