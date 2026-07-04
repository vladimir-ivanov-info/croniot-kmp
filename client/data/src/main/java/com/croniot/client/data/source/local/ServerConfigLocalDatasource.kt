package com.croniot.client.data.source.local

import kotlinx.coroutines.flow.Flow

interface ServerConfigLocalDatasource {
    suspend fun getServerIp(): Flow<String?>
    suspend fun saveServerIp(serverIp: String)
    fun getServerMode(): Flow<String?>
    suspend fun getCurrentServerMode(): Flow<String?>
    suspend fun saveServerMode(serverMode: String)
}
