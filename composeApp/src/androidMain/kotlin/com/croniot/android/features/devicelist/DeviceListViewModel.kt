package com.croniot.android.features.devicelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.usecases.LogoutUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.android.core.notifications.TaskNotificationManager
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class DeviceListViewModel(
    private val localDataRepository: LocalDataRepository,
    private val sensorDataRepository: SensorDataRepository,
    private val logOutUseCase: LogoutUseCase,
    private val startDeviceListenersUseCase: StartDeviceListenersUseCase,
    private val taskNotificationManager: TaskNotificationManager,
) : ViewModel() {

    private var devicesCollectors: MutableMap<String, Job> = mutableMapOf()

    val state: StateFlow<DeviceListState>
        field = MutableStateFlow(DeviceListState())

    val effects: SharedFlow<DeviceListEffect>
        field = MutableSharedFlow(replay = 0, extraBufferCapacity = 1)

    init {
        state
            .map { it.devices.map { d -> d.uuid } }
            .distinctUntilChanged()
            .onEach { uuids -> resubscribeToDevices(uuids) }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            runCatching { localDataRepository.getCurrentAccount() }
                .onSuccess { account ->
                    account?.let {
                        val devices = account.devices.filter { it.name.isNotEmpty() }
                        updateState { it.copy(devices = devices) }
                        devices.forEach { device ->
                            taskNotificationManager.startObserving(device.uuid)
                        }
                    }
                }
                .onFailure { logOut() }
        }
    }

    fun onIntent(intent: DeviceListIntent) {
        when (intent) {
            is DeviceListIntent.LogOut -> logOut()
            is DeviceListIntent.DeviceClicked -> effects.tryEmit(DeviceListEffect.NavigateToDevice(intent.deviceUuid))
        }
    }

    private fun updateState(transform: (DeviceListState) -> DeviceListState) {
        state.update(transform)
    }

    private fun resubscribeToDevices(uuids: List<String>) {
        val toCancel = devicesCollectors.keys - uuids.toSet()
        toCancel.forEach { uuid ->
            devicesCollectors.remove(uuid)?.cancel()
            updateState { it.copy(lastSeenMillis = it.lastSeenMillis - uuid) }
        }

        val toAdd = uuids - devicesCollectors.keys
        toAdd.forEach { uuid ->
            val job = observeMostRecentSensorMillis(uuid)
                .onStart { emit(0) }
                .onEach { ts -> updateState { it.copy(lastSeenMillis = it.lastSeenMillis + (uuid to ts)) } }
                .launchIn(viewModelScope)
            devicesCollectors[uuid] = job
        }
    }

    private fun observeMostRecentSensorMillis(deviceUuid: String): StateFlow<Long?> {
        return sensorDataRepository.devicesLatestSensorTimestamp
            .map { map -> map[deviceUuid] }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 1L,
            )
    }

    fun reconnectIfNeeded() = launchInVmScope {
        val account = localDataRepository.getCurrentAccount() ?: return@launchInVmScope
        startDeviceListenersUseCase(account.devices)
    }

    private fun logOut() = launchInVmScope {
        taskNotificationManager.stopAll()
        logOutUseCase()
        effects.tryEmit(DeviceListEffect.LogOut)
    }
}

sealed interface DeviceListEffect {
    data object LogOut : DeviceListEffect
    data class NavigateToDevice(val deviceUuid: String) : DeviceListEffect
}

sealed interface DeviceListIntent {
    data object LogOut : DeviceListIntent
    data class DeviceClicked(val deviceUuid: String) : DeviceListIntent
}

data class DeviceListState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val lastSeenMillis: Map<String, Long?> = emptyMap(),
)
