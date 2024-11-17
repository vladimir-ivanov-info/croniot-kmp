package com.croniot.android.presentation.configuration

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.ForegroundService
import com.croniot.android.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

//class ConfigurationViewModel : ViewModel() {
class ConfigurationViewModel(application: Application) : AndroidViewModel(application),
    KoinComponent {

    private val _foregroundServiceEnabled = MutableStateFlow(false)
    val foregroundServiceEnabled : StateFlow<Boolean> get() = _foregroundServiceEnabled

    lateinit var context : Context

    init {
        context = getApplication<Application>()
        loadConfigurationForegoundService()
    }

    private fun loadConfigurationForegoundService(){
        val configurationForegoundService = SharedPreferences.loadData(SharedPreferences.KEY_CONFIGURATION_FOREGROUND_SERVICE)
        if(configurationForegoundService != null){
            viewModelScope.launch {
                if(configurationForegoundService == "true"){
                    _foregroundServiceEnabled.emit(true)
                } else {
                    _foregroundServiceEnabled.emit(false)
                }
            }
        }
    }

    fun setConfigurationForegoundService(context2: Context, newValue : Boolean){

        //TODO
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }*/

        val newValueString = if (newValue) "true" else "false"

        if(newValue){
            //val serviceIntent = Intent(this, ForegroundService::class.java)
            val serviceIntent = Intent(context, ForegroundService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }

        SharedPreferences.saveData(SharedPreferences.KEY_CONFIGURATION_FOREGROUND_SERVICE, newValueString)

        viewModelScope.launch {
            _foregroundServiceEnabled.emit(newValue)
        }
    }

    fun setConfigurationForegroundService(context: Context, enableService: Boolean) {
        val serviceIntent = Intent(context, ForegroundService::class.java)

        if (enableService) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            context.stopService(serviceIntent)
        }
        viewModelScope.launch {
            _foregroundServiceEnabled.emit(enableService)
        }
    }


    // SharedPreferences.saveData(SharedPreferences.KEY_SERVER_MODE, newServerMode)

}