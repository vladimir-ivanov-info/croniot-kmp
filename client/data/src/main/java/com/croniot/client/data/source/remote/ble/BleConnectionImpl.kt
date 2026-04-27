package com.croniot.client.data.source.remote.ble

import Outcome
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import com.croniot.client.data.mappers.toModel
import com.croniot.client.data.source.remote.mappers.toDomain
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.SensorData
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import croniot.messages.MessageAddTask
import croniot.messages.MessageFactory
import croniot.models.dto.SensorDataDto
import croniot.models.dto.TaskDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

@SuppressLint("MissingPermission")
class BleConnectionImpl(
    override val deviceUuid: String,
    private val device: BluetoothDevice,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BleConnection {

    private val bridge = BleGattCallbackBridge()
    private var gatt: BluetoothGatt? = null
    private val gattMutex = Mutex()

    private val _connectionState = MutableStateFlow(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    override suspend fun connectAndAuthenticate(
        username: String,
        password: String,
    ): Outcome<Unit, BleError> = withContext(ioDispatcher) {
        try {
            _connectionState.value = BleConnectionState.Connecting
            val gattInstance = device.connectGatt(context, /* autoConnect = */ false, bridge)
                ?: return@withContext failed(BleError.Unknown("connectGatt returned null"))
            gatt = gattInstance

            val connected = withTimeoutOrNull(BleProfile.DEFAULT_OPERATION_TIMEOUT_MS) {
                bridge.connectionState.first { it == BluetoothProfile.STATE_CONNECTED }
            }
            if (connected == null) return@withContext failed(BleError.Timeout)
            _connectionState.value = BleConnectionState.Connected

            if (!gattInstance.discoverServices()) {
                return@withContext failed(BleError.Unknown("discoverServices returned false"))
            }
            val discoveryStatus = withTimeoutOrNull(BleProfile.DEFAULT_OPERATION_TIMEOUT_MS) {
                bridge.servicesDiscovered.receive()
            } ?: return@withContext failed(BleError.Timeout)
            if (discoveryStatus != BluetoothGatt.GATT_SUCCESS) {
                return@withContext failed(BleError.GattError(discoveryStatus))
            }

            val service = gattInstance.getService(BleProfile.SERVICE_UUID)
                ?: return@withContext failed(BleError.NotFound(deviceUuid))
            val authChar = service.getCharacteristic(BleProfile.CHARACTERISTIC_AUTH)
                ?: return@withContext failed(BleError.NotFound(deviceUuid))
            val sensorsChar = service.getCharacteristic(BleProfile.CHARACTERISTIC_SENSORS)
                ?: return@withContext failed(BleError.NotFound(deviceUuid))
            val tasksChar = service.getCharacteristic(BleProfile.CHARACTERISTIC_TASKS)
                ?: return@withContext failed(BleError.NotFound(deviceUuid))

            listOf(authChar, sensorsChar, tasksChar).forEach { char ->
                val status = enableNotifications(gattInstance, char)
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    return@withContext failed(BleError.GattError(status))
                }
            }

            _connectionState.value = BleConnectionState.Authenticating
            val authJson = MessageFactory.toJson(BleAuthRequest(username, password))
            val authStatus = writeWithAck(gattInstance, authChar, authJson.toByteArray(Charsets.UTF_8))
            if (authStatus != BluetoothGatt.GATT_SUCCESS) {
                return@withContext failed(BleError.GattError(authStatus))
            }

            val authResponseJson = withTimeoutOrNull(BleProfile.DEFAULT_OPERATION_TIMEOUT_MS) {
                bridge.notifications
                    .first { it.characteristicUuid == BleProfile.CHARACTERISTIC_AUTH }
                    .payload
            } ?: return@withContext failed(BleError.Timeout)

            val authResponse = runCatching {
                MessageFactory.fromJson<BleAuthResponse>(authResponseJson)
            }.getOrNull() ?: return@withContext failed(BleError.AuthFailed)

            if (!authResponse.ok) return@withContext failed(BleError.AuthFailed)

            _connectionState.value = BleConnectionState.Ready
            Outcome.Ok(Unit)
        } catch (e: SecurityException) {
            failed(BleError.PermissionDenied)
        } catch (e: Exception) {
            failed(BleError.Unknown(e.message))
        }
    }

    override fun observeSensorData(): Flow<SensorData> = bridge.notifications
        .filter { it.characteristicUuid == BleProfile.CHARACTERISTIC_SENSORS }
        .mapNotNull { event ->
            runCatching {
                MessageFactory.fromJsonWithZonedDateTime<SensorDataDto>(event.payload).toDomain()
            }.getOrNull()
        }

    override fun observeNewTasks(): Flow<Task> = bridge.notifications
        .filter { it.characteristicUuid == BleProfile.CHARACTERISTIC_TASKS }
        .mapNotNull { event ->
            runCatching {
                val frame = MessageFactory.fromJson<BleTaskFrame>(event.payload)
                if (frame.type != BleTaskFrameType.NEW_TASK) return@mapNotNull null
                val taskDto = MessageFactory.json.decodeFromJsonElement(TaskDto.serializer(), frame.data)
                taskDto.toModel().copy(deviceUuid = deviceUuid)
            }.getOrNull()
        }

    override fun observeTaskStateInfoEvents(): Flow<TaskStateInfoEvent> = bridge.notifications
        .filter { it.characteristicUuid == BleProfile.CHARACTERISTIC_TASKS }
        .mapNotNull { event ->
            runCatching {
                val frame = MessageFactory.fromJson<BleTaskFrame>(event.payload)
                if (frame.type != BleTaskFrameType.STATE_INFO_EVENT) return@mapNotNull null
                val payload = MessageFactory.json.decodeFromJsonElement(
                    BleStateInfoEventPayload.serializer(),
                    frame.data,
                )
                TaskStateInfoEvent(key = payload.key, info = payload.info.toModel())
            }.getOrNull()
        }

    override suspend fun sendNewTask(message: MessageAddTask): Outcome<Unit, BleError> =
        writeTaskFrame(
            BleTaskFrame(
                type = BleTaskFrameType.ADD_TASK,
                data = MessageFactory.json.encodeToJsonElement(MessageAddTask.serializer(), message),
            ),
        )

    override suspend fun requestTaskStateInfoSync(taskTypeUid: Long): Outcome<Unit, BleError> =
        writeTaskFrame(
            BleTaskFrame(
                type = BleTaskFrameType.REQUEST_SYNC,
                data = JsonObject(mapOf("taskTypeUid" to JsonPrimitive(taskTypeUid))),
            ),
        )

    override fun close() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {
            // Best-effort close.
        }
        gatt = null
        bridge.close()
        _connectionState.value = BleConnectionState.Disconnected
    }

    private suspend fun writeTaskFrame(frame: BleTaskFrame): Outcome<Unit, BleError> = withContext(ioDispatcher) {
        val gattInstance = gatt ?: return@withContext Outcome.Err(BleError.Unknown("Not connected"))
        val tasksChar = gattInstance
            .getService(BleProfile.SERVICE_UUID)
            ?.getCharacteristic(BleProfile.CHARACTERISTIC_TASKS)
            ?: return@withContext Outcome.Err(BleError.NotFound(deviceUuid))
        val payload = MessageFactory.toJson(frame).toByteArray(Charsets.UTF_8)
        val status = writeWithAck(gattInstance, tasksChar, payload)
        if (status == BluetoothGatt.GATT_SUCCESS) Outcome.Ok(Unit) else Outcome.Err(BleError.GattError(status))
    }

    private suspend fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ): Int = gattMutex.withLock {
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            return@withLock BluetoothGatt.GATT_FAILURE
        }
        val cccd = characteristic.getDescriptor(CCCD_UUID)
            ?: return@withLock BluetoothGatt.GATT_FAILURE
        val writeStatus = gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        if (writeStatus != BluetoothStatusCodes.SUCCESS) return@withLock BluetoothGatt.GATT_FAILURE
        val ack = withTimeoutOrNull(BleProfile.DEFAULT_OPERATION_TIMEOUT_MS) {
            bridge.descriptorWrites.receive()
        } ?: return@withLock BluetoothGatt.GATT_FAILURE
        ack.status
    }

    private suspend fun writeWithAck(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Int = gattMutex.withLock {
        val writeStatus = gatt.writeCharacteristic(
            characteristic,
            value,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        )
        if (writeStatus != BluetoothStatusCodes.SUCCESS) return@withLock BluetoothGatt.GATT_FAILURE
        val ack = withTimeoutOrNull(BleProfile.DEFAULT_OPERATION_TIMEOUT_MS) {
            bridge.characteristicWrites.receive()
        } ?: return@withLock BluetoothGatt.GATT_FAILURE
        ack.status
    }

    private fun failed(error: BleError): Outcome<Unit, BleError> {
        _connectionState.value = BleConnectionState.Failed
        return Outcome.Err(error)
    }

    companion object {
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
