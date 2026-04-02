package com.croniot.android.features.device.presentation

import androidx.lifecycle.ViewModel
import com.croniot.client.core.models.Device
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class DeviceScreenViewModel(
    private val localDataRepository: LocalDataRepository,
    private val fetchTasksUseCase: FetchTasksUseCase,
    private val startDeviceListenersUseCase: StartDeviceListenersUseCase,
) : ViewModel() {

    val state: StateFlow<DeviceState>
        field = MutableStateFlow<DeviceState>(DeviceState.Loading)

    fun onIntent(intent: DeviceIntent) {
        when (intent) {
            is DeviceIntent.Initialize -> {
                val current = state.value
                if (current is DeviceState.Content && current.device.uuid == intent.deviceUuid) return
                loadDevice(intent.deviceUuid)
            }
            is DeviceIntent.SelectTab -> {
                val current = state.value
                if (current is DeviceState.Content) {
                    state.update { current.copy(selectedTab = intent.index) }
                }
            }
        }
    }

    fun reconnectIfNeeded() = launchInVmScope {
        val account = localDataRepository.getCurrentAccount() ?: return@launchInVmScope
        startDeviceListenersUseCase(account.devices)
    }

    private fun loadDevice(deviceUuid: String) = launchInVmScope {
        state.value = DeviceState.Loading
        val account = localDataRepository.getCurrentAccount()
        if (account == null) {
            state.value = DeviceState.Error("No account found")
            return@launchInVmScope
        }
        val device = account.devices.find { it.uuid == deviceUuid }
        if (device == null) {
            state.value = DeviceState.Error("Device not found")
            return@launchInVmScope
        }
        state.value = DeviceState.Content(device = device)
        fetchTasksUseCase(deviceUuid)
    }
}

sealed interface DeviceState {
    data object Loading : DeviceState
    data class Content(val device: Device, val selectedTab: Int = 0) : DeviceState
    data class Error(val message: String) : DeviceState
}

sealed interface DeviceIntent {
    data class Initialize(val deviceUuid: String) : DeviceIntent
    data class SelectTab(val index: Int) : DeviceIntent
}