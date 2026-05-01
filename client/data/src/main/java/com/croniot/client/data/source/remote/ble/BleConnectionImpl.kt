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
import android.util.Log
import com.croniot.client.data.mappers.toModel
import com.croniot.client.data.source.remote.mappers.toDomain
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.SensorData
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import croniot.messages.MessageAddTask
import croniot.messages.MessageFactory
import croniot.models.TaskKey
import croniot.models.dto.TaskDto
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
            // ESP32 has sm_bonding=0: it never stores the LTK, so any bond Android saved
            // from a prior session will be rejected by the device → error 257 on reconnect.
            // Remove the stale bond first so the connection starts clean.
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                Log.d("BleConnection", "Eliminando bond obsoleto antes de reconectar...")
                removeBond(device)
                delay(600)
            }

            _connectionState.value = BleConnectionState.Connecting
            val gattInstance = device.connectGatt(
                context,
                false,
                bridge,
                BluetoothDevice.TRANSPORT_LE
            ) ?: return@withContext failed(BleError.Unknown("connectGatt returned null"))
            gatt = gattInstance

            val connected = withTimeoutOrNull(BleProfile.DEFAULT_OPERATION_TIMEOUT_MS) {
                bridge.connectionState.first { it == BluetoothProfile.STATE_CONNECTED }
            }
            if (connected == null) return@withContext failed(BleError.Timeout)
            _connectionState.value = BleConnectionState.Connected
            Log.d("BleConnection", "Conectado. Estado inicial: ${device.bondState}")

            // For bonded devices Android re-establishes the encrypted link asynchronously
            // after STATE_CONNECTED with no callback. discoverServices() called before the
            // cipher is ready returns error 257, so we wait here unconditionally.
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                Log.d("BleConnection", "Dispositivo vinculado — esperando establecimiento de cifrado...")
                delay(2000)
            }

            // MTU before pairing: larger MTU reduces SyncData chunks (507 bytes/chunk
            // at MTU 512 vs 18 bytes/chunk at default MTU 23).
            Log.d("BleConnection", "Solicitando MTU 512...")
            gattInstance.requestMtu(512)
            withTimeoutOrNull(5000) { bridge.mtuChanged.receive() }

            // 1. Manejo de Emparejamiento (PIN)
            // Esperamos un momento por si el dispositivo inicia la seguridad (Slave Security Request)
            var securityWait = 0
            while (device.bondState == BluetoothDevice.BOND_NONE && securityWait < 6) {
                delay(500)
                securityWait++
            }

            if (device.bondState == BluetoothDevice.BOND_NONE) {
                Log.d("BleConnection", "Dispositivo no inició PIN. Forzando vínculo...")
                device.createBond()
                // Android updates bondState asynchronously; without this delay the
                // BOND_BONDING check below fires before the system transitions the state,
                // causing discoverServices() to race with the security handshake → error 257.
                delay(1500)
            }

            if (device.bondState == BluetoothDevice.BOND_BONDING) {
                Log.d("BleConnection", "Esperando a que el usuario complete el PIN...")
                while (device.bondState == BluetoothDevice.BOND_BONDING) {
                    if (bridge.connectionState.value != BluetoothProfile.STATE_CONNECTED) {
                        Log.w("BleConnection", "Desconectado durante PIN.")
                        break
                    }
                    delay(1000)
                }
                Log.d("BleConnection", "PIN procesado. Estado final: ${device.bondState}")
                delay(3000) // Delay de "enfriamiento" crítico tras PIN
            }

            // 2. Descubrimiento de Servicios (Resiliente al Error 257)
            var discoveryStatus = -1
            var discoveryRetries = 0
            while (discoveryRetries < 4) {
                if (bridge.connectionState.value != BluetoothProfile.STATE_CONNECTED) {
                    Log.e("BleConnection", "Dispositivo desconectado antes del intento $discoveryRetries de descubrimiento.")
                    return@withContext failed(BleError.Unknown("Desconectado durante el descubrimiento de servicios"))
                }

                if (discoveryRetries > 0) {
                    Log.w("BleConnection", "Refrescando caché GATT por fallo previo...")
                    refreshDeviceCache(gattInstance)
                    delay(3000)
                }

                // Limpiar canal de resultados previos
                while (bridge.servicesDiscovered.tryReceive().isSuccess) { }

                Log.d("BleConnection", "Iniciando discoverServices (intento $discoveryRetries)...")
                if (!gattInstance.discoverServices()) {
                    Log.e("BleConnection", "discoverServices() rechazado por el sistema.")
                    delay(3000)
                    discoveryRetries++
                    continue
                }

                discoveryStatus = withTimeoutOrNull(15000) {
                    bridge.servicesDiscovered.receive()
                } ?: -1

                if (discoveryStatus == BluetoothGatt.GATT_SUCCESS) break
                
                Log.w("BleConnection", "Error en descubrimiento: $discoveryStatus. Reintentando...")
                delay(2000)
                discoveryRetries++
            }

            if (discoveryStatus != BluetoothGatt.GATT_SUCCESS) {
                return@withContext failed(BleError.GattError(discoveryStatus))
            }

            val services = gattInstance.services
            val service = gattInstance.getService(BleProfile.SERVICE_UUID)
                ?: run {
                    Log.e("BleConnection", "Servicio no encontrado. UUIDs: ${services.map { it.uuid }}")
                    return@withContext failed(BleError.NotFound(deviceUuid))
                }

            val authChar = service.getCharacteristic(BleProfile.CHARACTERISTIC_AUTH)
                ?: return@withContext failed(BleError.NotFound(deviceUuid))
            val sensorsChar = service.getCharacteristic(BleProfile.CHARACTERISTIC_SENSORS)
                ?: return@withContext failed(BleError.NotFound(deviceUuid))
            val taskProgressChar = service.getCharacteristic(BleProfile.CHARACTERISTIC_TASK_PROGRESS)
                ?: return@withContext failed(BleError.NotFound(deviceUuid))

            // 4. Habilitar Notificaciones
            listOf(authChar, sensorsChar, taskProgressChar).forEach { char ->
                val status = enableNotifications(gattInstance, char)
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e("BleConnection", "Error habilitando notificación en ${char.uuid}: $status")
                    return@withContext failed(BleError.GattError(status))
                }
            }

            // 5. Autenticación de Aplicación
            _connectionState.value = BleConnectionState.Authenticating
            val authJson = MessageFactory.toJson(BleAuthRequest(username, password))

            // Drain any stale auth notifications from a prior interrupted session
            while (bridge.authNotification.tryReceive().isSuccess) { }

            val authStatus = writeWithAck(gattInstance, authChar, authJson.toByteArray(Charsets.UTF_8))
            if (authStatus != BluetoothGatt.GATT_SUCCESS) {
                return@withContext failed(BleError.GattError(authStatus))
            }

            // Auth is a stub on the firmware side — any NOTIFY response (or timeout) counts
            // as success. Real credential validation will be added when the ESP32 implements it.
            Log.d("BleConnection", "Auth enviada, esperando respuesta...")
            val authResponseJson = withTimeoutOrNull(BleProfile.DEFAULT_OPERATION_TIMEOUT_MS) {
                bridge.authNotification.receive()
            }
            Log.d("BleConnection", "Auth response: ${authResponseJson ?: "timeout (stub mode)"}")

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
                val dto = MessageFactory.fromJson<BleSensorDataDto>(event.payload)
                val valueStr = (dto.value as? JsonPrimitive)?.content ?: dto.value.toString()
                val ts = dto.timestampMs
                    ?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
                    ?: ZonedDateTime.now()
                SensorData(
                    deviceUuid = deviceUuid,
                    sensorTypeUid = dto.sensorTypeUid,
                    value = valueStr,
                    timeStamp = ts,
                )
            }.onFailure {
                Log.w("BleConnection", "Sensor parse failed: ${it.message} | payload=${event.payload}")
            }.getOrNull()
        }

    override fun observeNewTasks(): Flow<Task> = bridge.notifications
        .filter { it.characteristicUuid == BleProfile.CHARACTERISTIC_TASK_PROGRESS }
        .mapNotNull { event ->
            runCatching {
                val frame = MessageFactory.fromJson<BleTaskFrame>(event.payload)
                if (frame.type != BleTaskFrameType.NEW_TASK) return@mapNotNull null
                val taskDto = MessageFactory.json.decodeFromJsonElement(TaskDto.serializer(), frame.data)
                taskDto.toModel().copy(deviceUuid = deviceUuid)
            }.getOrNull()
        }

    override fun observeTaskStateInfoEvents(): Flow<TaskStateInfoEvent> = bridge.notifications
        .filter { it.characteristicUuid == BleProfile.CHARACTERISTIC_TASK_PROGRESS }
        .mapNotNull { event ->
            runCatching {
                val frame = MessageFactory.fromJson<BleTaskFrame>(event.payload)
                if (frame.type != BleTaskFrameType.STATE_INFO_EVENT) return@mapNotNull null
                val p = MessageFactory.json.decodeFromJsonElement(
                    BleTaskStateEventPayload.serializer(),
                    frame.data,
                )
                val ts = p.timestampMs
                    ?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
                    ?: ZonedDateTime.now()
                TaskStateInfoEvent(
                    key = TaskKey(deviceUuid = deviceUuid, taskTypeUid = p.taskTypeUid, taskUid = p.taskUid),
                    info = TaskStateInfo(dateTime = ts, state = p.state, progress = p.progress, errorMessage = p.errorMessage),
                )
            }.onFailure {
                Log.w("BleConnection", "TaskStateInfo parse failed: ${it.message} | payload=${event.payload}")
            }.getOrNull()
        }

    override suspend fun sendNewTask(message: MessageAddTask): Outcome<Unit, BleError> =
        writeToChar(
            BleProfile.CHARACTERISTIC_TASK_COMMAND,
            BleTaskFrame(
                type = BleTaskFrameType.ADD_TASK,
                data = MessageFactory.json.encodeToJsonElement(MessageAddTask.serializer(), message),
            ),
        )

    override suspend fun requestTaskStateInfoSync(taskTypeUid: Long): Outcome<Unit, BleError> =
        writeToChar(
            BleProfile.CHARACTERISTIC_TASK_STATE_SYNC,
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

    private suspend fun writeToChar(charUuid: UUID, frame: BleTaskFrame): Outcome<Unit, BleError> = withContext(ioDispatcher) {
        val gattInstance = gatt ?: return@withContext Outcome.Err(BleError.Unknown("Not connected"))
        val characteristic = gattInstance
            .getService(BleProfile.SERVICE_UUID)
            ?.getCharacteristic(charUuid)
            ?: return@withContext Outcome.Err(BleError.NotFound(deviceUuid))
        val payload = MessageFactory.toJson(frame).toByteArray(Charsets.UTF_8)
        Log.d("BleConnection", "writeToChar $charUuid: ${payload.toString(Charsets.UTF_8)}")
        val status = writeWithAck(gattInstance, characteristic, payload)
        Log.d("BleConnection", "writeToChar $charUuid status=$status")
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

    override suspend fun syncSchema(cachedSchemaVersion: Long?): Outcome<BleSyncResult, BleError> =
        withContext(ioDispatcher) {
            try {
                val gattInstance = gatt
                    ?: return@withContext Outcome.Err(BleError.Unknown("Not connected"))
                val service = gattInstance.getService(BleProfile.SERVICE_UUID)
                    ?: return@withContext Outcome.Err(BleError.NotFound(deviceUuid))

                // Read DeviceInfo to get the current schemaVersion
                val deviceInfoChar = service.getCharacteristic(BleProfile.CHARACTERISTIC_DEVICE_INFO)
                    ?: return@withContext Outcome.Err(BleError.NotFound(deviceUuid))
                val deviceInfoBytes = readWithAck(gattInstance, deviceInfoChar)
                    ?: return@withContext Outcome.Err(BleError.Unknown("DeviceInfo read failed"))
                val deviceInfo = runCatching {
                    MessageFactory.fromJson<BleDeviceInfo>(deviceInfoBytes.toString(Charsets.UTF_8))
                }.getOrNull() ?: return@withContext Outcome.Err(BleError.Unknown("DeviceInfo parse failed"))

                Log.d("BleConnection", "DeviceInfo: schemaVersion=${deviceInfo.schemaVersion}, cached=$cachedSchemaVersion")

                if (cachedSchemaVersion != null && cachedSchemaVersion == deviceInfo.schemaVersion) {
                    return@withContext Outcome.Ok(BleSyncResult.UpToDate)
                }

                // Schema is stale or absent — perform sync
                Log.d("BleConnection", "Chars en servicio: ${service.characteristics.map { it.uuid }}")

                val syncDataChar = service.getCharacteristic(BleProfile.CHARACTERISTIC_SYNC_DATA)
                    ?: run {
                        Log.e("BleConnection", "SYNC_DATA no encontrada (UUID esperada: ${BleProfile.CHARACTERISTIC_SYNC_DATA})")
                        return@withContext Outcome.Err(BleError.NotFound(deviceUuid))
                    }
                val syncCommandChar = service.getCharacteristic(BleProfile.CHARACTERISTIC_SYNC_COMMAND)
                    ?: run {
                        Log.e("BleConnection", "SYNC_COMMAND no encontrada (UUID esperada: ${BleProfile.CHARACTERISTIC_SYNC_COMMAND})")
                        return@withContext Outcome.Err(BleError.NotFound(deviceUuid))
                    }

                Log.d("BleConnection", "Suscribiendo a SYNC_DATA (descriptores: ${syncDataChar.descriptors.map { it.uuid }})...")
                val subscribeStatus = enableNotifications(gattInstance, syncDataChar)
                Log.d("BleConnection", "SYNC_DATA subscribe status: $subscribeStatus")
                if (subscribeStatus != BluetoothGatt.GATT_SUCCESS) {
                    return@withContext Outcome.Err(BleError.GattError(subscribeStatus))
                }

                // Drain stale chunks from a previous interrupted sync
                while (bridge.syncDataChunks.tryReceive().isSuccess) { }

                Log.d("BleConnection", "Enviando START_SCHEMA_SYNC...")
                val writeStatus = writeWithAck(
                    gattInstance,
                    syncCommandChar,
                    "START_SCHEMA_SYNC".toByteArray(Charsets.UTF_8),
                )
                Log.d("BleConnection", "START_SCHEMA_SYNC write status: $writeStatus")
                if (writeStatus != BluetoothGatt.GATT_SUCCESS) {
                    return@withContext Outcome.Err(BleError.GattError(writeStatus))
                }

                // Accumulate chunks: [seq: uint8][total: uint8][data: bytes...]
                val chunkMap = mutableMapOf<Int, ByteArray>()
                var totalChunks = -1
                while (chunkMap.size < totalChunks.coerceAtLeast(1)) {
                    val timeoutMs = if (chunkMap.isEmpty()) BleProfile.DEFAULT_OPERATION_TIMEOUT_MS
                                    else BleProfile.SYNC_CHUNK_TIMEOUT_MS
                    val chunk = withTimeoutOrNull(timeoutMs) {
                        bridge.syncDataChunks.receive()
                    } ?: return@withContext Outcome.Err(BleError.Timeout)

                    if (totalChunks == -1) totalChunks = chunk.total
                    chunkMap[chunk.seq] = chunk.data
                    Log.d("BleConnection", "Sync chunk ${chunk.seq + 1}/$totalChunks recibido (${chunk.data.size} bytes)")
                }

                val schemaJson = (0 until totalChunks)
                    .mapNotNull { chunkMap[it] }
                    .fold(ByteArray(0)) { acc, bytes -> acc + bytes }
                    .toString(Charsets.UTF_8)

                Log.d("BleConnection", "Schema sync completo: ${schemaJson.length} chars, version=${deviceInfo.schemaVersion}")
                Outcome.Ok(BleSyncResult.Updated(deviceInfo.schemaVersion, schemaJson))
            } catch (e: SecurityException) {
                Outcome.Err(BleError.PermissionDenied)
            } catch (e: Exception) {
                Outcome.Err(BleError.Unknown(e.message))
            }
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

    private suspend fun readWithAck(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ): ByteArray? = gattMutex.withLock {
        if (!gatt.readCharacteristic(characteristic)) return@withLock null
        val ack = withTimeoutOrNull(BleProfile.DEFAULT_OPERATION_TIMEOUT_MS) {
            bridge.characteristicReads.receive()
        } ?: return@withLock null
        if (ack.status != BluetoothGatt.GATT_SUCCESS) return@withLock null
        ack.value
    }

    private fun removeBond(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod("removeBond")
            val success = method.invoke(device) as Boolean
            Log.d("BleConnection", "removeBond() invocado: $success")
        } catch (e: Exception) {
            Log.e("BleConnection", "Error al invocar removeBond() via reflexión", e)
        }
    }

    private fun refreshDeviceCache(gatt: BluetoothGatt) {
        try {
            val method = gatt.javaClass.getMethod("refresh")
            val success = method.invoke(gatt) as Boolean
            Log.d("BleConnection", "GATT refresh() invocado: $success")
        } catch (e: Exception) {
            Log.e("BleConnection", "Error al invocar refresh() via reflexión", e)
        }
    }

    companion object {
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
