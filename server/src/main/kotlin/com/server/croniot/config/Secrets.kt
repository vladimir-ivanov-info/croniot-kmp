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
    val jwtSecretCurrent: String,
    val jwtSecretPrevious: String?,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtAccessTokenTtlMinutes: Long,
    val jwtRefreshTokenTtlDays: Long,
    val dbPoolMaxSize: Int,
    val dbPoolMinIdle: Int,
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
                jwtSecretCurrent = resolve("CRONIOT_JWT_SECRET_CURRENT"),
                jwtSecretPrevious = resolveOptional("CRONIOT_JWT_SECRET_PREVIOUS"),
                jwtIssuer = resolveOrDefault("CRONIOT_JWT_ISSUER", "croniot-server"),
                jwtAudience = resolveOrDefault("CRONIOT_JWT_AUDIENCE", "croniot-client"),
                jwtAccessTokenTtlMinutes = resolveLong("CRONIOT_JWT_ACCESS_TTL_MINUTES", 15L),
                jwtRefreshTokenTtlDays = resolveLong("CRONIOT_JWT_REFRESH_TTL_DAYS", 30L),
                dbPoolMaxSize = resolveInt("CRONIOT_DB_POOL_MAX_SIZE", 8),
                dbPoolMinIdle = resolveInt("CRONIOT_DB_POOL_MIN_IDLE", 2),
            )
        }

        private fun resolve(name: String): String {
            return System.getenv(name)
                ?: localProperties.getProperty(name)
                ?: ""
        }

        private fun resolveOptional(name: String): String? {
            val raw = System.getenv(name) ?: localProperties.getProperty(name)
            return raw?.takeIf { it.isNotBlank() }
        }

        private fun resolveOrDefault(name: String, default: String): String {
            return resolveOptional(name) ?: default
        }

        private fun resolveLong(name: String, default: Long): Long {
            return resolveOptional(name)?.toLongOrNull() ?: default
        }

        private fun resolveInt(name: String, default: Int): Int {
            return resolveOptional(name)?.toIntOrNull() ?: default
        }
    }
}
