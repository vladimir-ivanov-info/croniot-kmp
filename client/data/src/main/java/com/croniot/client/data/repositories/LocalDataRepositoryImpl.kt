package com.croniot.client.data.repositories

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.Device
import com.croniot.client.core.util.DevicePropertiesController
import com.croniot.client.data.source.local.LocalDatasource
import com.croniot.client.domain.repositories.LocalDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LocalDataRepositoryImpl(
    private val localDatasource: LocalDatasource,
) : LocalDataRepository {

    override suspend fun getCurrentRoute(): String? {
        return localDatasource.getCurrentRoute()
    }

    override suspend fun saveCurrentRoute(route: String) {
        return localDatasource.saveCurrentRoute(route)
    }

    override suspend fun getCurrentPassword(): String? {
        return localDatasource.getCurrentPassword()
    }

    override suspend fun getLocalDeviceUuid(): String? {
        return localDatasource.getLocalDeviceUuid()
    }

    override suspend fun getIsForegroundServiceEnabled(): Boolean {
        return localDatasource.getIsForegroundServiceEnabled()
    }

    override suspend fun saveIsForegroundServiceEnabled(foregroundServiceEnabled: Boolean) {
        return localDatasource.saveIsForegroundServiceEnabled(foregroundServiceEnabled)
    }

    override suspend fun generateAndSaveDeviceUuidIfNotExists() {
        localDatasource.generateAndSaveDeviceUuidIfNotExists()
    }

    override suspend fun getCurrentScreen(): String? {
        return localDatasource.getCurrentScreen()
    }

    override suspend fun saveCurrentScreen(screen: String) {
        return localDatasource.saveCurrentScreen(screen)
    }

    override suspend fun getCurrentAccount(): Account? {
        return localDatasource.getCurrentAccount()
    }

    override suspend fun getSelectedDevice(): Device? {
        return localDatasource.getSelectedDevice()
    }

    override suspend fun saveSelectedDevice(device: Device) {
        localDatasource.saveSelectedDevice(device)
    }

    override suspend fun getLocalDeviceToken(): String? {
        return localDatasource.getLocalDeviceToken()
    }

    override suspend fun saveCurrentAccount(account: Account?) {
        localDatasource.saveCurrentAccount(account)
    }

    override suspend fun saveEmail(email: String) {
        localDatasource.saveEmail(email)
    }

    override suspend fun savePassword(password: String) {
        localDatasource.savePassword(password) // TODO plain text, fix later
    }

    override suspend fun saveToken(token: String) {
        localDatasource.saveToken(token)
    }

    override fun getDeviceProperties(): Map<String, String> {
        return DevicePropertiesController.getDeviceDetails()
    }

    fun clearAccount() {
        // TODO
        // repositoryScope.launch {
        //    com.croniot.client.data.source.local.DataStoreController.saveAccount(null)
        // }
    }

    override suspend fun clearAllCacheExceptDeviceUuid() {
        localDatasource.clearAllCacheExceptDeviceUuid()
    }

    override fun getServerMode(): Flow<String?> {
        return localDatasource.getServerMode()
    }

    /*override suspend fun getServerIp(): Flow<String?> {
        return localDatasource.getServerIp()
    }*/

    override suspend fun getServerIp(): String? {
        return localDatasource.getServerIp().first()
    }
}
