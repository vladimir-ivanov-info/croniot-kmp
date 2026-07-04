package com.croniot.client.data.source.local

interface AppPreferencesLocalDatasource {
    suspend fun getIsForegroundServiceEnabled(): Boolean
    suspend fun saveIsForegroundServiceEnabled(enabled: Boolean)
    suspend fun clearAllCacheExceptDeviceUuid()
    suspend fun getAppSessionMode(): String?
    suspend fun saveAppSessionMode(mode: String?)
}
