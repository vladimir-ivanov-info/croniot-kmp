package com.croniot.android.features.device.presentation

import androidx.lifecycle.ViewModel
import com.croniot.client.core.models.Device
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class DeviceScreenViewModel(
    private val localDataRepository: LocalDataRepository,
    private val fetchTasksUseCase: FetchTasksUseCase,
) : ViewModel() {

    val state: StateFlow<DeviceState>
        field = MutableStateFlow(DeviceState())

    fun onIntent(intent: DeviceIntent) {
        when (intent) {
            is DeviceIntent.Initialize -> showDevice(intent.deviceUuid)
            is DeviceIntent.SelectTab -> state.update { it.copy(selectedTab = intent.index) }
        }
    }

    private fun showDevice(deviceUuid: String) = launchInVmScope {
        val account = localDataRepository.getCurrentAccount() ?: return@launchInVmScope
        val device = account.devices.find { it.uuid == deviceUuid } ?: return@launchInVmScope
        state.update { it.copy(device = device) }
        fetchTasksUseCase(deviceUuid)
    }
}

data class DeviceState(
    val device: Device? = null,
    val selectedTab: Int = 0,
)

sealed interface DeviceIntent {
    data class Initialize(val deviceUuid: String) : DeviceIntent
    data class SelectTab(val index: Int) : DeviceIntent
}