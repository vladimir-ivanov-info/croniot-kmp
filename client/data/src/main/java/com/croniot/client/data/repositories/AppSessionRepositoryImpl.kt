package com.croniot.client.data.repositories

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.session.AppSession
import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.LocalDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppSessionRepositoryImpl(
    private val localDataRepository: LocalDataRepository,
    appScope: CoroutineScope,
) : AppSessionRepository {

    private val _session = MutableStateFlow<AppSession>(AppSession.None)
    override val session: StateFlow<AppSession> = _session.asStateFlow()

    init {
        appScope.launch { loadInitialSession() }
    }

    private suspend fun loadInitialSession() {
        _session.value = when (localDataRepository.getAppSessionMode()) {
            MODE_SERVER -> {
                val account = localDataRepository.getCurrentAccount()
                if (account != null) AppSession.Server(account) else AppSession.None
            }
            MODE_BLE -> AppSession.BleOnly
            else -> AppSession.None
        }
    }

    override suspend fun activateServerSession(account: Account) {
        localDataRepository.saveAppSessionMode(MODE_SERVER)
        _session.value = AppSession.Server(account)
    }

    override suspend fun activateBleOnlyMode() {
        localDataRepository.saveAppSessionMode(MODE_BLE)
        _session.value = AppSession.BleOnly
    }

    override suspend fun clear() {
        localDataRepository.saveAppSessionMode(null)
        _session.value = AppSession.None
    }

    private companion object {
        const val MODE_SERVER = "server"
        const val MODE_BLE = "ble"
    }
}
