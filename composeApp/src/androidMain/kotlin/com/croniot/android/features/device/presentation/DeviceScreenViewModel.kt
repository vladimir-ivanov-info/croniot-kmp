package com.croniot.android.features.device.presentation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.models.Device
import com.croniot.client.data.repositories.LocalDataRepository
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.presentation.constants.UiConstants.ROUTE_DEVICE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class DeviceScreenViewModel(
    private val localDataRepository: LocalDataRepository,
    private val fetchTasksUseCase: FetchTasksUseCase,
) : ViewModel(), KoinComponent {

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> get() = _currentTab

    private val _device = mutableStateOf<Device?>(null)
    val device: State<Device?> = _device

    fun initialize(deviceUuid: String) {
        viewModelScope.launch {
            val account = localDataRepository.getCurrentAccount()
            if (account != null) {
                val selectedDevice = account.devices.find { it.uuid == deviceUuid }
                if (selectedDevice != null) {
                    _device.value = selectedDevice
                }

                fetchTasksUseCase(deviceUuid) // TODO rename to preCacheTasks
            }
        }
    }

    fun updateCurrentTab(newTab: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _currentTab.emit(newTab)
        }
    }

    fun saveCurrentScreen() {
        viewModelScope.launch {
            localDataRepository.saveCurrentScreen(ROUTE_DEVICE)
        }
    }
}
