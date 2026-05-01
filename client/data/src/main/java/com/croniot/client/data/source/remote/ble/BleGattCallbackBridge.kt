package com.croniot.client.data.source.remote.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.util.Log
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
    val mtuChanged: Channel<MtuAck> = Channel(capacity = Channel.UNLIMITED)
    val characteristicReads: Channel<ReadAck> = Channel(capacity = Channel.UNLIMITED)
    val descriptorWrites: Channel<DescriptorAck> = Channel(capacity = Channel.UNLIMITED)
    val characteristicWrites: Channel<WriteAck> = Channel(capacity = Channel.UNLIMITED)
    // Channel instead of SharedFlow: buffers the NOTIFY even before the consumer is
    // registered, avoiding the race where the ESP32 responds faster than coroutine setup.
    val authNotification: Channel<String> = Channel(capacity = Channel.UNLIMITED)
    val syncDataChunks: Channel<SyncDataChunk> = Channel(capacity = Channel.UNLIMITED)

    private val _notifications = MutableSharedFlow<NotificationEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    val notifications: SharedFlow<NotificationEvent> = _notifications.asSharedFlow()

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        Log.d("BleBridge", "onConnectionStateChange: status=$status, newState=$newState")
        _connectionState.value = newState
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.d("BleBridge", "onServicesDiscovered: status=$status")
        servicesDiscovered.trySend(status)
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        Log.d("BleBridge", "onMtuChanged: mtu=$mtu, status=$status")
        mtuChanged.trySend(MtuAck(mtu, status))
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
    ) {
        Log.d("BleBridge", "onDescriptorWrite: char=${descriptor.characteristic.uuid}, desc=${descriptor.uuid}, status=$status")
        descriptorWrites.trySend(
            DescriptorAck(
                characteristicUuid = descriptor.characteristic.uuid,
                descriptorUuid = descriptor.uuid,
                status = status,
            ),
        )
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        Log.d("BleBridge", "onCharacteristicRead: char=${characteristic.uuid}, status=$status, bytes=${value.size}")
        characteristicReads.trySend(ReadAck(characteristic.uuid, value, status))
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        Log.d("BleBridge", "onCharacteristicWrite: char=${characteristic.uuid}, status=$status")
        characteristicWrites.trySend(WriteAck(characteristic.uuid, status))
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (characteristic.uuid == BleProfile.CHARACTERISTIC_AUTH) {
            authNotification.trySend(value.toString(Charsets.UTF_8))
            return
        }
        if (characteristic.uuid == BleProfile.CHARACTERISTIC_SYNC_DATA) {
            // Binary protocol: [seq: uint8][total: uint8][data: bytes...]
            if (value.size >= 2) {
                syncDataChunks.trySend(
                    SyncDataChunk(
                        seq = value[0].toInt() and 0xFF,
                        total = value[1].toInt() and 0xFF,
                        data = value.copyOfRange(2, value.size),
                    )
                )
            }
            return
        }
        val payload = value.toString(Charsets.UTF_8)
        Log.d("BleBridge", "onCharacteristicChanged: char=${characteristic.uuid}, payload=$payload")
        _notifications.tryEmit(NotificationEvent(characteristicUuid = characteristic.uuid, payload = payload))
    }

    fun close() {
        servicesDiscovered.close()
        mtuChanged.close()
        characteristicReads.close()
        descriptorWrites.close()
        characteristicWrites.close()
        authNotification.close()
        syncDataChunks.close()
    }

    data class MtuAck(val mtu: Int, val status: Int)
    data class ReadAck(val characteristicUuid: UUID, val value: ByteArray, val status: Int)
    data class WriteAck(val characteristicUuid: UUID, val status: Int)
    data class DescriptorAck(
        val characteristicUuid: UUID,
        val descriptorUuid: UUID,
        val status: Int,
    )
    data class NotificationEvent(val characteristicUuid: UUID, val payload: String)
    data class SyncDataChunk(val seq: Int, val total: Int, val data: ByteArray)
}
