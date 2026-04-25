package com.croniot.android.features.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import com.croniot.client.data.source.remote.http.HostHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConfigurationScreenViewModel(
    private val serverConfigLocalDatasource: ServerConfigLocalDatasource,
    private val hostHolder: HostHolder,
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
            //is ConfigurationIntent.ToggleServerMode -> toggleServerMode()
            //is ConfigurationIntent.SetForegroundService -> setForegroundService(intent.enabled)
            is ConfigurationIntent.SetServerIp -> setServerIp(intent.ip)
        }
    }

    private fun loadForegroundService() = viewModelScope.launch {
        //val enabled = serverConfigLocalDatasource.getIsForegroundServiceEnabled()
        //state.update { it.copy(foregroundServiceEnabled = enabled) }
    }

    private fun loadServerIp() = viewModelScope.launch {
        serverConfigLocalDatasource.getServerIp().collect { ip ->
            state.update { it.copy(serverIp = ip ?: ServerConfig.SERVER_IP_REMOTE) }
        }
    }

    private fun observeServerMode() = viewModelScope.launch {
        /*serverConfigLocalDatasource.getCurrentServerMode().collect { mode ->
            mode ?: return@collect
            state.update { it.copy(serverMode = mode) }
            hostHolder.host = if (mode == "remote") {
                ServerConfig.SERVER_ADDRESS_REMOTE
            } else {
                ServerConfig.SERVER_ADDRESS_LOCAL
            }
        }*/
    }

    private fun toggleServerMode() = viewModelScope.launch {
        //TODO
        /*val newMode = if (state.value.serverMode == "remote") "local" else "remote"
        ServerConfig.SERVER_ADDRESS = if (newMode == "remote") {
            ServerConfig.SERVER_ADDRESS_REMOTE
        } else {
            ServerConfig.SERVER_ADDRESS_LOCAL
        }
        serverConfigLocalDatasource.saveServerMode(newMode)*/
    }

    private fun setServerIp(ip: String) = viewModelScope.launch {
        state.update { it.copy(serverIp = ip) }
        serverConfigLocalDatasource.saveServerIp(ip)

        hostHolder.host = ip
    }

    private fun setForegroundService(enabled: Boolean) = viewModelScope.launch {
        //state.update { it.copy(foregroundServiceEnabled = enabled) }
        //serverConfigLocalDatasource.saveIsForegroundServiceEnabled(enabled)
    }
}

data class ConfigurationState(
    //val foregroundServiceEnabled: Boolean = false,
    //val serverMode: String = "remote",
    val serverIp: String = "",
)

sealed interface ConfigurationIntent {
    //data object ToggleServerMode : ConfigurationIntent
    //data class SetForegroundService(val enabled: Boolean) : ConfigurationIntent
    data class SetServerIp(val ip: String) : ConfigurationIntent
}
