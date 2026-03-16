package com.croniot.android.features.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.data.source.local.LocalDatasource
import com.croniot.client.data.source.remote.http.HostSelectionInterceptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConfigurationScreenViewModel(
    private val localDatasource: LocalDatasource,
    private val hostInterceptor: HostSelectionInterceptor,
) : ViewModel() {

    val state: StateFlow<ConfigurationState>
        field = MutableStateFlow(ConfigurationState())

    init {
        loadForegroundService()
        loadServerIp()
        observeServerMode()
    }

    fun onIntent(intent: ConfigurationIntent) {
        when (intent) {
            is ConfigurationIntent.ToggleServerMode -> toggleServerMode()
            is ConfigurationIntent.SetForegroundService -> setForegroundService(intent.enabled)
        }
    }

    private fun loadForegroundService() = viewModelScope.launch {
        val enabled = localDatasource.getIsForegroundServiceEnabled()
        state.update { it.copy(foregroundServiceEnabled = enabled) }
    }

    private fun loadServerIp() = viewModelScope.launch {
        localDatasource.getServerIp().collect { ip ->
            state.update { it.copy(serverIp = ip ?: "192.168.50.163") }
        }
    }

    private fun observeServerMode() = viewModelScope.launch {
        localDatasource.getCurrentServerMode().collect { mode ->
            mode ?: return@collect
            state.update { it.copy(serverMode = mode) }
            hostInterceptor.host = if (mode == "remote") {
                ServerConfig.SERVER_ADDRESS_REMOTE
            } else {
                ServerConfig.SERVER_ADDRESS_LOCAL
            }
        }
    }

    private fun toggleServerMode() = viewModelScope.launch {
        val newMode = if (state.value.serverMode == "remote") "local" else "remote"
        ServerConfig.SERVER_ADDRESS = if (newMode == "remote") {
            ServerConfig.SERVER_ADDRESS_REMOTE
        } else {
            ServerConfig.SERVER_ADDRESS_LOCAL
        }
        localDatasource.saveServerMode(newMode)
    }

    private fun setForegroundService(enabled: Boolean) = viewModelScope.launch {
        state.update { it.copy(foregroundServiceEnabled = enabled) }
        localDatasource.saveIsForegroundServiceEnabled(enabled)
    }
}

data class ConfigurationState(
    val foregroundServiceEnabled: Boolean = false,
    val serverMode: String = "remote",
    val serverIp: String = "",
)

sealed interface ConfigurationIntent {
    data object ToggleServerMode : ConfigurationIntent
    data class SetForegroundService(val enabled: Boolean) : ConfigurationIntent
}
