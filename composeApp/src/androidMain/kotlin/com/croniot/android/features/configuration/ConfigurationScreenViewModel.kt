package com.croniot.android.features.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.ServerConfig
import com.croniot.client.data.source.local.LocalDatasource
import com.croniot.client.data.source.remote.HostSelectionInterceptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConfigurationScreenViewModel(
    private val localDatasource: LocalDatasource,
) : ViewModel(), KoinComponent {

    private val _foregroundServiceEnabled = MutableStateFlow(false)
    val foregroundServiceEnabled: StateFlow<Boolean> get() = _foregroundServiceEnabled

    private var _serverMode = MutableStateFlow("remote")
    val serverMode: StateFlow<String> get() = _serverMode

    private val hostInterceptor: HostSelectionInterceptor by inject() // TODO move to constructor

    init {

        viewModelScope.launch {
            loadConfigurationForegoundService()

            localDatasource.getCurrentServerMode().collect { serverMode ->
                serverMode?.let {
                    _serverMode.value = serverMode

                    if (serverMode == "remote") {
                        // NetworkModule.reloadRetrofitRemote()
                        // NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_REMOTE)
                        hostInterceptor.host = ServerConfig.SERVER_ADDRESS_REMOTE
                    } else {
                        // NetworkModule.reloadRetrofitLocal()
                        // NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_LOCAL)
                        hostInterceptor.host = ServerConfig.SERVER_ADDRESS_LOCAL
                    }
                }
            }

            /*DataStoreController.loadData(DataStoreController.KEY_SERVER_MODE).collect { serverMode ->
                serverMode?.let {
                    _serverMode.value = serverMode


                    if (serverMode == "remote") {
                        //NetworkModule.reloadRetrofitRemote()
                        //NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_REMOTE)
                        hostInterceptor.host = ServerConfig.SERVER_ADDRESS_REMOTE
                    } else {
                        //NetworkModule.reloadRetrofitLocal()
                        //NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_LOCAL)
                        hostInterceptor.host = ServerConfig.SERVER_ADDRESS_LOCAL
                    }
                }
            }*/
        }
    }

   /* private val _foregroundServiceEnabled = MutableStateFlow(false)
    val foregroundServiceEnabled: StateFlow<Boolean> get() = _foregroundServiceEnabled

    init {

    }*/

    private fun loadConfigurationForegoundService() {
        val configurationForegoundService = runBlocking {
            // DataStoreController.loadData(DataStoreController.KEY_CONFIGURATION_FOREGROUND_SERVICE).first()
            localDatasource.getIsForegroundServiceEnabled()
        }

        // configurationForegoundService?.let {
        viewModelScope.launch {
            if (configurationForegoundService) {
                _foregroundServiceEnabled.emit(true)
            } else {
                _foregroundServiceEnabled.emit(false)
            }
        }
        // }
    }

    fun changeServerMode() {
        viewModelScope.launch {
            val currentServerMode = _serverMode.value
            var newServerMode = "remote"

            if (currentServerMode == "remote") {
                newServerMode = "local"
                ServerConfig.SERVER_ADDRESS = ServerConfig.SERVER_ADDRESS_LOCAL
                // NetworkModule.reloadRetrofitLocal()
                // NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_LOCAL)

                hostInterceptor.host = ServerConfig.SERVER_ADDRESS_LOCAL
            } else {
                ServerConfig.SERVER_ADDRESS = ServerConfig.SERVER_ADDRESS_REMOTE
                // NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_REMOTE)
                // NetworkModule.reloadRetrofitRemote()

                hostInterceptor.host = ServerConfig.SERVER_ADDRESS_REMOTE
            }

            viewModelScope.launch {
                //    DataStoreController.saveData(DataStoreController.KEY_SERVER_MODE, newServerMode)
                localDatasource.saveServerMode(newServerMode)
            }
        }
    }

    fun setConfigurationForegoundService(newValue: Boolean) {
       /* val newValueString = if (newValue) "true" else "false"

        if (newValue) {
            val serviceIntent = Intent(context, ForegroundService::class.java)

            context.startForegroundService(serviceIntent)
        } else {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }

        viewModelScope.launch {
            _foregroundServiceEnabled.emit(newValue)
            //DataStoreController.saveData(DataStoreController.KEY_CONFIGURATION_FOREGROUND_SERVICE, newValueString)
            localDataRepository.saveIsForegroundServiceEnabled(newValue)
        }*/
    }
}
