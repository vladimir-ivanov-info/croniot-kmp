package com.croniot.android.features.deviceslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.core.data.source.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import croniot.models.dto.DeviceDto
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class DevicesListViewModel() : ViewModel(), KoinComponent {

    private val accountRepository: AccountRepository = get()

    private val _devices = MutableStateFlow<List<DeviceDto>>(emptyList())
    val devices: StateFlow<List<DeviceDto>> get() = _devices

    private val _lastOnlineUpdates = mutableMapOf<String, Long>()

    init {
        init1()
    }

    fun init1(){
        println()

        viewModelScope.launch {
            accountRepository.account.collect { account ->
                account?.let {
                    val devices = account.devices
                    updateDevices(devices.filter { device -> device.name.isNotEmpty() })
                } //TODO else

                if (account == null) {
                    updateDevices(emptyList())
                }
            }
        }
    }

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