package com.croniot.android.features.devicelist

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.models.Device
import com.croniot.client.data.repositories.LocalDataRepository
import com.croniot.client.data.repositories.SessionRepository
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.usecases.LogoutUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent

class DeviceListViewModel(
    private val localDataRepository: LocalDataRepository,
    private val sensorDataRepository: SensorDataRepository,
    private val logOutUseCase: LogoutUseCase,
    private val sessionRepository: SessionRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), KoinComponent {

    // private val _devices = MutableStateFlow<List<Device>>(emptyList())
    // val devices: StateFlow<List<Device>> get() = _devices

    private val _lastSeenMillis = MutableStateFlow<Map<String, Long?>>(emptyMap())
    val lastSeenMillis: StateFlow<Map<String, Long?>> = _lastSeenMillis

    // Para cancelar colectores cuando cambie la lista de devices
    private var devicesCollectors: MutableMap<String, Job> = mutableMapOf()

    companion object {
        private const val KEY_DEVICE_LIST_STATE = "device_list_state"
    }

    private val _state = MutableStateFlow(
        savedStateHandle.get<DeviceListState>(KEY_DEVICE_LIST_STATE) ?: DeviceListState(),
    )
    val state: StateFlow<DeviceListState> = _state.asStateFlow()

    /*private val _events = MutableSharedFlow<DevicesListUiEvent>(
        replay = 0,                 // <- MUY importante para one-time
        extraBufferCapacity = 1     // opcional, para tryEmit sin suspender
    )
    val events = _events*/

    private val _effects = Channel<DeviceListEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<DeviceListEffect> = _effects.receiveAsFlow()

    /*init {
        //TODO
        viewModelScope.launch {
            val currentAccount = localDataRepository.getCurrentAccount()

            currentAccount?.let {
                val devices = currentAccount.devices
                val filteredDevices = devices.filter { device -> device.name.isNotEmpty() }

                //_devices.emit(filteredDevices)
                updateState { it.copy(devices = devices) }

            } ?: run {
                logOut()
            }
        }
    }

    init {
        viewModelScope.launch {
            val currentAccount = localDataRepository.getCurrentAccount()

            currentAccount?.let { account ->

                updateState { it.copy(devices = account.devices) }

                // Observa cambios en la lista de devices y (re)suscribe colectores
                state
                    .map { it.devices.map { d -> d.uuid } }   // solo uuids
                    .distinctUntilChanged()
                    .onEach { uuids ->
                        resubscribeToDevices(uuids)
                    }
                    .launchIn(viewModelScope)
            }
        }
    }*/

    init {
        // 1) Observa cambios en la lista de devices (una sola vez)
        state
            .map { it.devices.map { d -> d.uuid } } // solo uuids
            .distinctUntilChanged()
            .onEach { uuids -> resubscribeToDevices(uuids) }
            .launchIn(viewModelScope)

        // 2) Carga inicial del account y mete sus devices en el estado
        viewModelScope.launch {
            runCatching { localDataRepository.getCurrentAccount() }
                .onSuccess { account ->
                    account?.let {

                        val devices = account.devices.filter { device -> device.name.isNotEmpty() }

                        updateState { st -> st.copy(devices = devices) }
                        // No hace falta llamar manualmente a resubscribeToDevices:
                        // el watcher de arriba se activará con estos devices.
                    }
                }
                .onFailure {
                    // opcional: updateState { it.copy(error = ...) } o log
                    logOut()
                }
        }
    }

    private inline fun updateState(transform: (DeviceListState) -> DeviceListState) {
        _state.update { current ->
            val newState = transform(current)
            // TODO for production: savedStateHandle[KEY_STATE] = newState.copy(password = "")
            savedStateHandle[KEY_DEVICE_LIST_STATE] = newState
            newState
        }
    }

    fun onAction(action: DeviceListIntent) {
        when (action) {
            is DeviceListIntent.DeviceListUpdated -> updateState { it.copy(devices = action.devices) }
            is DeviceListIntent.LogOut -> {
                /*viewModelScope.launch {
                    sessionRepository.clearAllExceptDeviceUuid()
                }*/
                logOut()
            }
            else -> Unit
        }
    }

    private fun sendEffect(effect: DeviceListEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    private fun resubscribeToDevices(uuids: List<String>) {
        // 1) Cancela colectores de devices que ya no están
        val toCancel = devicesCollectors.keys - uuids.toSet()
        toCancel.forEach { uuid ->
            devicesCollectors.remove(uuid)?.cancel()
            // Limpia también su valor del mapa (opcional)
            _lastSeenMillis.update { it - uuid }
        }

        // 2) Suscríbete a devices nuevos
        val toAdd = uuids - devicesCollectors.keys
        toAdd.forEach { uuid ->
            val job = observeMostRecentSensorMillis(uuid)
                .onStart { emit(0) } // hasta que llegue el primero
                .onEach { ts ->
                    _lastSeenMillis.update { old -> old + (uuid to ts) }
                }
                .launchIn(viewModelScope)

            devicesCollectors[uuid] = job
        }
    }

    private fun observeMostRecentSensorMillis(deviceUuid: String): StateFlow<Long?> {
        // TODO
        /*return sensorDataRepository.observeSensorDataInsertions(deviceUuid) // <- Flow<Long>
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = -1L
            )*/

        return sensorDataRepository.devicesLatestSensorTimestamp
            .map { map -> map[deviceUuid] } // extrae solo el valor de esa clave
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 1L,
            )

        /*sensorDataRepository.observeSensorData(deviceUuid, sensorUid).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SensorData(
                deviceUuid = deviceUuid,
                sensorTypeUid = sensorUid,
                value = Constants.PARAMETER_VALUE_UNDEFINED, //TODO
                timeStamp = ZonedDateTime.now()
            )
        )

        return MutableStateFlow<Long>(value = 123) //TODO*/
    }

   /* fun saveSelectedDevice(device: Device){
        viewModelScope.launch {
//            localDataRepository.saveSelectedDevice(device)
        }
    }*/

    private fun logOut() {
        viewModelScope.launch {
            logOutUseCase()

            sendEffect(DeviceListEffect.LogOut)
        }
    }
}

sealed interface DeviceListEffect {
    data object LogOut : DeviceListEffect
}

sealed interface DeviceListIntent {
    data class DeviceListUpdated(val devices: List<Device>) : DeviceListIntent
    data object LogOut : DeviceListIntent
    data class DeviceClicked(val deviceUuid: String) : DeviceListIntent
}

@Parcelize
data class DeviceListState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
) : Parcelable
