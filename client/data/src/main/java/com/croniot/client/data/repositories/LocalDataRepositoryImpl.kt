package com.croniot.client.data.repositories

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.Device
import com.croniot.client.core.util.DevicePropertiesController
import com.croniot.client.data.source.local.AppPreferencesLocalDatasource
import com.croniot.client.data.source.local.AuthLocalDatasource
import com.croniot.client.data.source.local.DeviceLocalDatasource
import com.croniot.client.data.source.local.NavigationLocalDatasource
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import com.croniot.client.domain.repositories.LocalDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LocalDataRepositoryImpl(
    private val navigationLocalDatasource: NavigationLocalDatasource,
    private val authLocalDatasource: AuthLocalDatasource,
    private val deviceLocalDatasource: DeviceLocalDatasource,
    private val appPreferencesLocalDatasource: AppPreferencesLocalDatasource,
    private val serverConfigLocalDatasource: ServerConfigLocalDatasource,
) : LocalDataRepository {

    override suspend fun getCurrentRoute(): String? {
        return navigationLocalDatasource.getCurrentRoute()
    }

    override suspend fun saveCurrentRoute(route: String) {
        return navigationLocalDatasource.saveCurrentRoute(route)
    }

    override suspend fun getLocalDeviceUuid(): String? {
        return deviceLocalDatasource.getLocalDeviceUuid()
    }

    override suspend fun getIsForegroundServiceEnabled(): Boolean {
        return appPreferencesLocalDatasource.getIsForegroundServiceEnabled()
    }

    override suspend fun saveIsForegroundServiceEnabled(foregroundServiceEnabled: Boolean) {
        return appPreferencesLocalDatasource.saveIsForegroundServiceEnabled(foregroundServiceEnabled)
    }

    override suspend fun generateAndSaveDeviceUuidIfNotExists() {
        deviceLocalDatasource.generateAndSaveDeviceUuidIfNotExists()
    }

    override suspend fun getCurrentScreen(): String? {
        return navigationLocalDatasource.getCurrentScreen()
    }

    override suspend fun saveCurrentScreen(screen: String) {
        return navigationLocalDatasource.saveCurrentScreen(screen)
    }

    override suspend fun getCurrentAccount(): Account? {
        return authLocalDatasource.getCurrentAccount()
    }

    override suspend fun getSelectedDevice(): Device? {
        return deviceLocalDatasource.getSelectedDevice()
    }

    override suspend fun saveSelectedDevice(device: Device) {
        deviceLocalDatasource.saveSelectedDevice(device)
    }

    override suspend fun getLocalDeviceToken(): String? {
        return deviceLocalDatasource.getLocalDeviceToken()
    }

    override suspend fun saveCurrentAccount(account: Account?) {
        authLocalDatasource.saveCurrentAccount(account)
    }

    override suspend fun saveEmail(email: String) {
        authLocalDatasource.saveEmail(email)
    }

    override fun getDeviceProperties(): Map<String, String> {
        return DevicePropertiesController.getDeviceDetails()
    }

    override suspend fun clearAllCacheExceptDeviceUuid() {
        appPreferencesLocalDatasource.clearAllCacheExceptDeviceUuid()
    }

    override fun getServerMode(): Flow<String?> {
        return serverConfigLocalDatasource.getServerMode()
    }

    override suspend fun getAppSessionMode(): String? {
        return appPreferencesLocalDatasource.getAppSessionMode()
    }

    override suspend fun saveAppSessionMode(mode: String?) {
        appPreferencesLocalDatasource.saveAppSessionMode(mode)
    }

    /*override suspend fun getServerIp(): Flow<String?> {
        return serverConfigLocalDatasource.getServerIp()
    }*/

    override suspend fun getServerIp(): String? {
        return serverConfigLocalDatasource.getServerIp().first()
    }
}
