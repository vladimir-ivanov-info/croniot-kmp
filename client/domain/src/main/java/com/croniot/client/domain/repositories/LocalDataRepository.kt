package com.croniot.client.domain.repositories

import com.croniot.client.core.models.Account
import com.croniot.client.core.models.Device
import kotlinx.coroutines.flow.Flow

interface LocalDataRepository { // TODO god class, but will be delegated to Room in the future

    suspend fun getCurrentRoute(): String?

    suspend fun saveCurrentRoute(route: String)

    suspend fun getCurrentPassword(): String?

    suspend fun getLocalDeviceUuid(): String?

    suspend fun getIsForegroundServiceEnabled(): Boolean

    suspend fun saveIsForegroundServiceEnabled(foregroundServiceEnabled: Boolean)

    suspend fun getServerAddress(): String

    suspend fun saveServerAddress(serverAddress: String)

    suspend fun generateAndSaveDeviceUuidIfNotExists()

    suspend fun getCurrentScreen(): String?

    suspend fun saveCurrentScreen(screen: String)

    suspend fun getCurrentAccount(): Account?

    suspend fun getSelectedDevice(): Device?

    suspend fun saveSelectedDevice(device: Device)
    // TODO resetSelectedDevice -> remove value from DataStore or store null

    suspend fun getLocalDeviceToken(): String?

    suspend fun saveCurrentAccount(account: Account?)

    suspend fun saveEmail(email: String)

    suspend fun savePassword(password: String)

    suspend fun saveToken(token: String)

    fun getDeviceProperties(): Map<String, String>

    suspend fun clearAllCacheExceptDeviceUuid()

    fun getServerMode(): Flow<String?>
}
