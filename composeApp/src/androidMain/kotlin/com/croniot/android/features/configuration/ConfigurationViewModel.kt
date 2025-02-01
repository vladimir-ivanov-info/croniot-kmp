package com.croniot.android.features.configuration

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.core.data.source.local.DataStoreController
import com.croniot.android.core.services.ForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConfigurationViewModel(application: Application) : AndroidViewModel(application), KoinComponent {

    val context: Context by inject()

    private val _foregroundServiceEnabled = MutableStateFlow(false)
    val foregroundServiceEnabled: StateFlow<Boolean> get() = _foregroundServiceEnabled

    init {
        loadConfigurationForegoundService()
    }

    private fun loadConfigurationForegoundService() {
        val configurationForegoundService = runBlocking {
            DataStoreController.loadData(DataStoreController.KEY_CONFIGURATION_FOREGROUND_SERVICE).first()
        }

        configurationForegoundService?.let {
            viewModelScope.launch {
                if (configurationForegoundService == "true") {
                    _foregroundServiceEnabled.emit(true)
                } else {
                    _foregroundServiceEnabled.emit(false)
                }
            }
        }
    }

    fun setConfigurationForegoundService(newValue: Boolean) {
        val newValueString = if (newValue) "true" else "false"

        if (newValue) {
            val serviceIntent = Intent(context, ForegroundService::class.java)

            context.startForegroundService(serviceIntent)
        } else {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }

        viewModelScope.launch {
            _foregroundServiceEnabled.emit(newValue)
            DataStoreController.saveData(DataStoreController.KEY_CONFIGURATION_FOREGROUND_SERVICE, newValueString)
        }
    }
}
