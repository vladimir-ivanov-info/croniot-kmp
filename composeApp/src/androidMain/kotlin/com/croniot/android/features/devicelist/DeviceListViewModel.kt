package com.croniot.android.features.devicelist

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.models.Device
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.usecases.LogoutUseCase
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
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

class DeviceListViewModel(
    private val localDataRepository: LocalDataRepository,
    private val sensorDataRepository: SensorDataRepository,
    private val logOutUseCase: LogoutUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val KEY_DEVICE_LIST_STATE = "device_list_state"
    }

    private var devicesCollectors: MutableMap<String, Job> = mutableMapOf()

    val state: StateFlow<DeviceListState>
        field = MutableStateFlow(
            savedStateHandle.get<DeviceListState>(KEY_DEVICE_LIST_STATE) ?: DeviceListState()
        )

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
        state.update { current ->
            val newState = transform(current)
            savedStateHandle[KEY_DEVICE_LIST_STATE] = newState
            newState
        }
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

    private fun logOut() = launchInVmScope {
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

@Parcelize
data class DeviceListState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    @IgnoredOnParcel
    val lastSeenMillis: Map<String, Long?> = emptyMap(),
) : Parcelable
