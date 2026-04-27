package com.croniot.client.domain.errors

sealed interface BleError {
    data object PermissionDenied : BleError
    data object BluetoothOff : BleError
    data object BluetoothNotSupported : BleError
    data object Timeout : BleError
    data object AuthFailed : BleError
    data object RequiresPairing : BleError
    data class NotFound(val deviceUuid: String) : BleError
    data class GattError(val status: Int) : BleError
    data class Unknown(val message: String?) : BleError
}
