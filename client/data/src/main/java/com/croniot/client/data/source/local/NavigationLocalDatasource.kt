package com.croniot.client.data.source.local

interface NavigationLocalDatasource {
    suspend fun getCurrentRoute(): String?
    suspend fun saveCurrentRoute(route: String)
    suspend fun getCurrentScreen(): String?
    suspend fun saveCurrentScreen(screen: String)
}
