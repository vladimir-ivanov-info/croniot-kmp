package com.croniot.client.data.source.remote.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BleGattCallbackBridge : BluetoothGattCallback() {

    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState.asStateFlow()

    val servicesDiscovered: Channel<Int> = Channel(capacity = Channel.UNLIMITED)
    val descriptorWrites: Channel<DescriptorAck> = Channel(capacity = Channel.UNLIMITED)
    val characteristicWrites: Channel<WriteAck> = Channel(capacity = Channel.UNLIMITED)

    private val _notifications = MutableSharedFlow<NotificationEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    val notifications: SharedFlow<NotificationEvent> = _notifications.asSharedFlow()

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        _connectionState.value = newState
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        servicesDiscovered.trySend(status)
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
    ) {
        descriptorWrites.trySend(
            DescriptorAck(
                characteristicUuid = descriptor.characteristic.uuid,
                descriptorUuid = descriptor.uuid,
                status = status,
            ),
        )
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        characteristicWrites.trySend(WriteAck(characteristic.uuid, status))
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        _notifications.tryEmit(
            NotificationEvent(
                characteristicUuid = characteristic.uuid,
                payload = value.toString(Charsets.UTF_8),
            ),
        )
    }

    fun close() {
        servicesDiscovered.close()
        descriptorWrites.close()
        characteristicWrites.close()
    }

    data class WriteAck(val characteristicUuid: UUID, val status: Int)
    data class DescriptorAck(
        val characteristicUuid: UUID,
        val descriptorUuid: UUID,
        val status: Int,
    )
    data class NotificationEvent(val characteristicUuid: UUID, val payload: String)
}
