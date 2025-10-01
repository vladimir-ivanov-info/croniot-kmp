package com.croniot.client.data.source.local

import com.croniot.client.core.models.Account
import com.croniot.client.core.models.Device
import kotlinx.coroutines.flow.Flow

interface LocalDatasource {

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

    suspend fun getLocalDeviceToken(): String?

    suspend fun saveCurrentAccount(account: Account?)

    suspend fun saveEmail(email: String)

    suspend fun savePassword(password: String)

    suspend fun saveToken(token: String)

    fun getServerMode(): Flow<String?>

    suspend fun getCurrentServerMode(): Flow<String?>

    suspend fun saveServerMode(serverMode: String)

    suspend fun clearAllCacheExceptDeviceUuid()
}
