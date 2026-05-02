package com.croniot.client.features.blediscovery.presentation

import Outcome
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.data.source.remote.ble.BlePermissionsHelper
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.ble.DiscoveredBleDevice
import com.croniot.client.domain.models.ble.KnownBleDevice
import com.croniot.client.domain.usecases.ble.ActivateBleOnlyModeUseCase
import com.croniot.client.domain.usecases.ble.ConnectBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ForgetBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ObserveKnownBleDevicesUseCase
import com.croniot.client.domain.usecases.ble.PairBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ScanBleDevicesUseCase
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class BleDiscoveryViewModel(
    private val scanBleDevicesUseCase: ScanBleDevicesUseCase,
    private val observeKnownBleDevicesUseCase: ObserveKnownBleDevicesUseCase,
    private val pairBleDeviceUseCase: PairBleDeviceUseCase,
    private val connectBleDeviceUseCase: ConnectBleDeviceUseCase,
    private val forgetBleDeviceUseCase: ForgetBleDeviceUseCase,
    private val activateBleOnlyModeUseCase: ActivateBleOnlyModeUseCase,
    private val permissionsHelper: BlePermissionsHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(BleDiscoveryState())
    val state: StateFlow<BleDiscoveryState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BleDiscoveryEffect>(replay = 0, extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    private val nearbyFlow: StateFlow<List<DiscoveredBleDevice>> =
        scanBleDevicesUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val knownFlow: StateFlow<List<KnownBleDevice>> =
        observeKnownBleDevicesUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshPermissionStatus()
        observeDeviceLists()
    }

    fun onAction(intent: BleDiscoveryIntent) {
        when (intent) {
            BleDiscoveryIntent.PermissionsGranted -> onPermissionsGranted()
            BleDiscoveryIntent.RefreshPermissionStatus -> refreshPermissionStatus()
            is BleDiscoveryIntent.PairRequested -> showPairDialog(intent.uuid, intent.displayName)
            BleDiscoveryIntent.PairDialogDismissed -> dismissPairDialog()
            is BleDiscoveryIntent.UsernameChanged -> _state.update {
                it.copy(pairing = it.pairing?.copy(username = intent.value))
            }
            is BleDiscoveryIntent.PasswordChanged -> _state.update {
                it.copy(pairing = it.pairing?.copy(password = intent.value))
            }
            BleDiscoveryIntent.PairConfirmed -> confirmPair()
            is BleDiscoveryIntent.ConnectKnown -> connectKnown(intent.uuid)
            is BleDiscoveryIntent.ForgetKnown -> forgetKnown(intent.uuid)
        }
    }

    private fun refreshPermissionStatus() {
        _state.update {
            it.copy(
                permissionsGranted = permissionsHelper.allGranted(),
                missingPermissions = permissionsHelper.missingPermissions(),
            )
        }
    }

    private fun onPermissionsGranted() {
        _state.update { it.copy(permissionsGranted = true, missingPermissions = emptyList()) }
    }

    private fun observeDeviceLists() = launchInVmScope {
        combine(nearbyFlow, knownFlow) { nearby, known ->
            val knownUuids = known.map { it.uuid }.toSet()
            val newNearby = nearby.filter { it.uuid !in knownUuids }
            newNearby to known
        }.collect { (nearby, known) ->
            _state.update { it.copy(nearby = nearby, known = known) }
        }
    }

    private fun showPairDialog(uuid: String, displayName: String) {
        _state.update {
            it.copy(pairing = PairingState(uuid = uuid, displayName = displayName))
        }
    }

    private fun dismissPairDialog() {
        _state.update { it.copy(pairing = null) }
    }

    private fun confirmPair() = launchInVmScope {
        val pairing = _state.value.pairing ?: return@launchInVmScope
        if (pairing.isSubmitting) return@launchInVmScope
        _state.update { it.copy(pairing = pairing.copy(isSubmitting = true, error = null)) }

        when (val result = pairBleDeviceUseCase(pairing.uuid, pairing.username, pairing.password)) {
            is Outcome.Ok -> {
                activateBleOnlyModeUseCase()
                _state.update { it.copy(pairing = null) }
                _effects.tryEmit(BleDiscoveryEffect.NavigateToDevice(result.value.uuid))
            }
            is Outcome.Err -> {
                _state.update {
                    it.copy(pairing = pairing.copy(isSubmitting = false, error = result.error.toUserMessage()))
                }
            }
        }
    }

    private fun connectKnown(uuid: String) = launchInVmScope {
        _state.update { it.copy(busyUuid = uuid) }
        when (val result = connectBleDeviceUseCase(uuid)) {
            is Outcome.Ok -> {
                activateBleOnlyModeUseCase()
                _state.update { it.copy(busyUuid = null) }
                _effects.tryEmit(BleDiscoveryEffect.NavigateToDevice(result.value.uuid))
            }
            is Outcome.Err -> {
                _state.update { it.copy(busyUuid = null) }
                _effects.tryEmit(BleDiscoveryEffect.ShowSnackbar(result.error.toUserMessage()))
            }
        }
    }

    private fun forgetKnown(uuid: String) = launchInVmScope {
        forgetBleDeviceUseCase(uuid)
    }
}

private fun BleError.toUserMessage(): String = when (this) {
    BleError.PermissionDenied -> "Permisos BLE denegados."
    BleError.BluetoothOff -> "Bluetooth desactivado."
    BleError.BluetoothNotSupported -> "Este dispositivo no soporta BLE."
    BleError.Timeout -> "Tiempo de espera agotado."
    BleError.AuthFailed -> "Credenciales inválidas."
    BleError.BondingFailed -> "Error de vinculación (PIN incorrecto o rechazo)."
    BleError.RequiresPairing -> "El dispositivo requiere emparejamiento."
    is BleError.NotFound -> "Dispositivo no encontrado: $deviceUuid"
    is BleError.GattError -> "Error GATT: $status"
    is BleError.Unknown -> message ?: "Error desconocido."
}

data class BleDiscoveryState(
    val permissionsGranted: Boolean = false,
    val missingPermissions: List<String> = emptyList(),
    val nearby: List<DiscoveredBleDevice> = emptyList(),
    val known: List<KnownBleDevice> = emptyList(),
    val busyUuid: String? = null,
    val pairing: PairingState? = null,
)

data class PairingState(
    val uuid: String,
    val displayName: String,
    //val username: String = "",
    //val password: String = "",
    val username: String = "user123",
    val password: String = "123456",
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

sealed interface BleDiscoveryIntent {
    data object PermissionsGranted : BleDiscoveryIntent
    data object RefreshPermissionStatus : BleDiscoveryIntent
    data class PairRequested(val uuid: String, val displayName: String) : BleDiscoveryIntent
    data object PairDialogDismissed : BleDiscoveryIntent
    data class UsernameChanged(val value: String) : BleDiscoveryIntent
    data class PasswordChanged(val value: String) : BleDiscoveryIntent
    data object PairConfirmed : BleDiscoveryIntent
    data class ConnectKnown(val uuid: String) : BleDiscoveryIntent
    data class ForgetKnown(val uuid: String) : BleDiscoveryIntent
}

sealed interface BleDiscoveryEffect {
    data class NavigateToDevice(val deviceUuid: String) : BleDiscoveryEffect
    data class ShowSnackbar(val message: String) : BleDiscoveryEffect
}
