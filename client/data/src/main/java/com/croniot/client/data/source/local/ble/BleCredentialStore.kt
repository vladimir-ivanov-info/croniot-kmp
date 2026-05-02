package com.croniot.client.data.source.local.ble

interface BleCredentialStore {
    suspend fun save(deviceUuid: String, username: String, password: String)
    suspend fun get(deviceUuid: String): BleCredentials?
    suspend fun forget(deviceUuid: String)
    suspend fun forgetAll()
}

data class BleCredentials(
    val username: String,
    val password: String,
)
