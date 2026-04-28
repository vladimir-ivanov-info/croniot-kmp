package com.croniot.testing.fakes

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.repositories.LocalDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLocalDataRepository(
    deviceUuid: String? = "fake-device-uuid",
    deviceToken: String? = null,
    account: Account? = null,
    selectedDevice: Device? = null,
) : LocalDataRepository {

    private var currentRoute: String? = null
    private var currentScreen: String? = null
    private var foregroundServiceEnabled: Boolean = false
    private var deviceUuid: String? = deviceUuid
    private var deviceToken: String? = deviceToken
    private var account: Account? = account
    private var selectedDevice: Device? = selectedDevice
    private val serverMode = MutableStateFlow<String?>(null)
    private var serverIp: String? = null

    override suspend fun getCurrentRoute(): String? = currentRoute
    override suspend fun saveCurrentRoute(route: String) { currentRoute = route }
    override suspend fun getCurrentScreen(): String? = currentScreen
    override suspend fun saveCurrentScreen(screen: String) { currentScreen = screen }

    override suspend fun getLocalDeviceUuid(): String? = deviceUuid
    override suspend fun generateAndSaveDeviceUuidIfNotExists() {
        if (deviceUuid == null) deviceUuid = "generated-uuid"
    }
    override suspend fun getLocalDeviceToken(): String? = deviceToken

    override suspend fun getIsForegroundServiceEnabled(): Boolean = foregroundServiceEnabled
    override suspend fun saveIsForegroundServiceEnabled(foregroundServiceEnabled: Boolean) {
        this.foregroundServiceEnabled = foregroundServiceEnabled
    }

    override suspend fun getCurrentAccount(): Account? = account
    override suspend fun saveCurrentAccount(account: Account?) { this.account = account }
    override suspend fun saveEmail(email: String) {}

    override suspend fun getSelectedDevice(): Device? = selectedDevice
    override suspend fun saveSelectedDevice(device: Device) { selectedDevice = device }

    override fun getDeviceProperties(): Map<String, String> = emptyMap()

    override suspend fun clearAllCacheExceptDeviceUuid() {
        currentRoute = null
        currentScreen = null
        account = null
        selectedDevice = null
    }

    override fun getServerMode(): Flow<String?> = serverMode
    override suspend fun getServerIp(): String? = serverIp
}
