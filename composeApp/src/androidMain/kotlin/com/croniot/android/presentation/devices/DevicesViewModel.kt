package com.croniot.android.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.GlobalViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import croniot.models.dto.DeviceDto
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class DevicesViewModel : ViewModel(), KoinComponent {

    private val _devices = MutableStateFlow<List<DeviceDto>>(emptyList())
    val devices: StateFlow<List<DeviceDto>> get() = _devices

    private val _lastOnlineUpdates = mutableMapOf<String, Long>()

    private val globalViewModel: GlobalViewModel = get()


    fun uninit(){
        _devices.value = emptyList()
    }

    fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000L) // 1 second delay
                emitTimerTick()
            }
        }
    }

    fun listenToDevicesIfNeeded(){

        val account = globalViewModel.account.value
        if(account != null){
            updateDevices(account.devices.filter{ it.name.isNotEmpty() }.toList()) //TODO for now we leave this filter
        }
    }

    private suspend fun emitTimerTick() {
        val updatedDevices = _devices.value.map { device ->
            val lastOnlineUpdate = _lastOnlineUpdates[device.uuid] ?: device.lastOnlineMillis
            device.copy(lastOnlineMillis = lastOnlineUpdate)
        }
        _devices.emit(updatedDevices)
    }

    fun updateDevices(devices: List<DeviceDto>){
        viewModelScope.launch {
            _devices.emit(devices)
        }
    }

    fun updateDeviceOnlineStatus(deviceUuid: String) {
        val updatedTime = System.currentTimeMillis()
        _lastOnlineUpdates[deviceUuid] = updatedTime
    }
}