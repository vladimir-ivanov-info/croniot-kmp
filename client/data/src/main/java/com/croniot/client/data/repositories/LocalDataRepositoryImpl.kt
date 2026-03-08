package com.croniot.client.data.repositories

import com.croniot.client.core.models.Account
import com.croniot.client.core.models.Device
import com.croniot.client.core.util.DevicePropertiesController
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.data.source.local.LocalDatasource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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

    override suspend fun getServerAddress(): String {
        return localDatasource.getServerAddress()
    }

    override suspend fun saveServerAddress(serverAddress: String) {
        localDatasource.saveServerAddress(serverAddress)
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

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun saveCurrentAccount(account: Account?) {
        repositoryScope.launch {
            localDatasource.saveCurrentAccount(account)
        }
    }

    override fun saveEmail(email: String) {
        repositoryScope.launch {
            localDatasource.saveEmail(email)
        }
    }

    override fun savePassword(password: String) {
        repositoryScope.launch {
            localDatasource.savePassword(password) //TODO plain text, fix later
        }
    }

    override fun saveToken(token: String) {
        repositoryScope.launch {
            localDatasource.saveToken(token)
        }
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
}
