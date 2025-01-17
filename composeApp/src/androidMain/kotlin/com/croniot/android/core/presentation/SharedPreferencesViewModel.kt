package com.croniot.android.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.app.Global
import com.croniot.android.core.data.source.local.SharedPreferences
import com.croniot.android.core.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

//TODO turn into controller, shouldn't be a viewmodel
class SharedPreferencesViewModel  : ViewModel(), KoinComponent {

    private var _serverMode = MutableStateFlow("remote")
    val serverMode : StateFlow<String> get() = _serverMode

    fun loadCurrentServerMode(){
        val serverModeFromSharedPreferences = SharedPreferences.loadData(SharedPreferences.KEY_SERVER_MODE)
        if(serverModeFromSharedPreferences != null){
            viewModelScope.launch {
                _serverMode.emit(serverModeFromSharedPreferences)
            }
        }
    }

    fun changeServerMode(){
        val currentServerMode = _serverMode.value
        var newServerMode = "remote"

        if(currentServerMode == "remote"){
            newServerMode = "local"
            Global.SERVER_ADDRESS = Global.SERVER_ADDRESS_LOCAL
            NetworkModule.reloadRetrofitLocal()
        } else {
            Global.SERVER_ADDRESS = Global.SERVER_ADDRESS_REMOTE
            NetworkModule.reloadRetrofitRemote()
        }
        SharedPreferences.saveData(SharedPreferences.KEY_SERVER_MODE, newServerMode)
        viewModelScope.launch {
            _serverMode.emit(newServerMode)
        }
    }
}