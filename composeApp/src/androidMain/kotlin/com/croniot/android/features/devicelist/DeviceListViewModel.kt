package com.croniot.android.features.devicelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.TransportKind
import com.croniot.client.domain.models.session.AppSession
import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.usecases.LogoutUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.client.domain.usecases.ble.ForgetBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ObserveKnownBleDevicesUseCase
import com.croniot.android.core.notifications.TaskNotificationManager
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceListViewModel(
    private val localDataRepository: LocalDataRepository,
    private val sensorDataRepository: SensorDataRepository,
    private val tasksRepository: TasksRepository,
    private val logOutUseCase: LogoutUseCase,
    private val startDeviceListenersUseCase: StartDeviceListenersUseCase,
    private val taskNotificationManager: TaskNotificationManager,
    private val appSessionRepository: AppSessionRepository,
    private val observeKnownBleDevicesUseCase: ObserveKnownBleDevicesUseCase,
    private val forgetBleDeviceUseCase: ForgetBleDeviceUseCase,
) : ViewModel() {

    private var devicesCollectors: MutableMap<String, Job> = mutableMapOf()

    val state: StateFlow<DeviceListState>
        field = MutableStateFlow(DeviceListState())

    val effects: SharedFlow<DeviceListEffect>
        field = MutableSharedFlow(replay = 0, extraBufferCapacity = 1)

    init {
        appSessionRepository.session
            .flatMapLatest { session ->
                when (session) {
                    is AppSession.Server -> flowOf(
                        DeviceListContent(
                            mode = TransportKind.CLOUD,
                            devices = session.account.devices.filter { it.name.isNotEmpty() },
                            inRangeUuids = emptySet(),
                        ),
                    )
                    AppSession.BleOnly -> observeKnownBleDevicesUseCase().map { known ->
                        DeviceListContent(
                            mode = TransportKind.BLE,
                            devices = known.map { entry ->
                                Device(
                                    uuid = entry.uuid,
                                    name = entry.displayName,
                                    description = "",
                                    transport = TransportKind.BLE,
                                )
                            },
                            inRangeUuids = known.filter { it.isInRange }.map { it.uuid }.toSet(),
                        )
                    }
                    AppSession.None -> flowOf(DeviceListContent())
                }
            }
            .onEach { content ->
                updateState {
                    it.copy(
                        mode = content.mode,
                        devices = content.devices,
                        inRangeUuids = content.inRangeUuids,
                    )
                }
                content.devices.forEach { device ->
                    if (content.mode == TransportKind.CLOUD) {
                        taskNotificationManager.startObserving(device.uuid)
                    }
                }
            }
            .launchIn(viewModelScope)

        state
            .map { it.devices.map { d -> d.uuid } }
            .distinctUntilChanged()
            .onEach { uuids -> resubscribeToDevices(uuids) }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: DeviceListIntent) {
        when (intent) {
            is DeviceListIntent.LogOut -> logOut()
            is DeviceListIntent.DeviceClicked ->
                effects.tryEmit(DeviceListEffect.NavigateToDevice(intent.deviceUuid))
            is DeviceListIntent.GoToBleDiscovery ->
                effects.tryEmit(DeviceListEffect.NavigateToBleDiscovery)
            is DeviceListIntent.ForgetBleDevice -> forgetBle(intent.deviceUuid)
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
            launchInVmScope {
                sensorDataRepository.stopListeningFor(uuid)
                tasksRepository.stopListeningFor(uuid)
            }
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
        if (state.value.mode != TransportKind.CLOUD) return@launchInVmScope
        val account = localDataRepository.getCurrentAccount() ?: return@launchInVmScope
        startDeviceListenersUseCase(account.devices)
    }

    private fun forgetBle(deviceUuid: String) = launchInVmScope {
        forgetBleDeviceUseCase(deviceUuid)
    }

    private fun logOut() = launchInVmScope {
        taskNotificationManager.stopAll()
        logOutUseCase()
        effects.tryEmit(DeviceListEffect.LogOut)
    }
}

private data class DeviceListContent(
    val mode: TransportKind = TransportKind.CLOUD,
    val devices: List<Device> = emptyList(),
    val inRangeUuids: Set<String> = emptySet(),
)

sealed interface DeviceListEffect {
    data object LogOut : DeviceListEffect
    data class NavigateToDevice(val deviceUuid: String) : DeviceListEffect
    data object NavigateToBleDiscovery : DeviceListEffect
}

sealed interface DeviceListIntent {
    data object LogOut : DeviceListIntent
    data class DeviceClicked(val deviceUuid: String) : DeviceListIntent
    data object GoToBleDiscovery : DeviceListIntent
    data class ForgetBleDevice(val deviceUuid: String) : DeviceListIntent
}

data class DeviceListState(
    val mode: TransportKind = TransportKind.CLOUD,
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val lastSeenMillis: Map<String, Long?> = emptyMap(),
    val inRangeUuids: Set<String> = emptySet(),
)
