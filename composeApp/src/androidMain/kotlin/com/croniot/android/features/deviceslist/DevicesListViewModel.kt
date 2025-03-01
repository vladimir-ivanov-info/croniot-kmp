package com.croniot.android.features.deviceslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.core.data.source.repository.AccountRepository
import com.croniot.android.core.data.source.repository.SensorDataRepository
import com.croniot.android.domain.model.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class DevicesListViewModel() : ViewModel(), KoinComponent {

    private val accountRepository: AccountRepository = get()

    private val sensorDataRepositoryImpl: SensorDataRepository = get()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> get() = _devices

    init {
        viewModelScope.launch {
            accountRepository.account.collect { account ->
                account?.let {
                    val devices = account.devices
                    updateDevices(devices.filter { device -> device.name.isNotEmpty() })
                } // TODO else

                if (account == null) {
                    updateDevices(emptyList())
                }
            }
        }
    }

    // TODO
    fun uninit() {
        _devices.value = emptyList()
    }

    private fun updateDevices(devices: List<Device>) {
        viewModelScope.launch {
            _devices.emit(devices)
        }
    }

    fun observeMostRecentSensorMillis(deviceUuid: String): StateFlow<Long> {
        return sensorDataRepositoryImpl.observeSensorDataInsertions(deviceUuid)
    }
}
