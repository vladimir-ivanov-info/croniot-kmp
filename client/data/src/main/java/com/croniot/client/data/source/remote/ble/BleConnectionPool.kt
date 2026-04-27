package com.croniot.client.data.source.remote.ble

import Outcome
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.croniot.client.domain.errors.BleError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

interface BleConnectionPool {
    suspend fun getOrConnect(
        deviceUuid: String,
        device: BluetoothDevice,
        username: String,
        password: String,
    ): Outcome<BleConnection, BleError>

    fun get(deviceUuid: String): BleConnection?

    suspend fun close(deviceUuid: String)

    suspend fun closeAll()
}

class BleConnectionPoolImpl(
    private val context: Context,
    private val maxConnections: Int = DEFAULT_MAX_CONNECTIONS,
    private val connectionFactory: (deviceUuid: String, device: BluetoothDevice) -> BleConnection = { uuid, dev ->
        BleConnectionImpl(uuid, dev, context)
    },
) : BleConnectionPool {

    private val connections = ConcurrentHashMap<String, BleConnection>()
    private val mutex = Mutex()

    override suspend fun getOrConnect(
        deviceUuid: String,
        device: BluetoothDevice,
        username: String,
        password: String,
    ): Outcome<BleConnection, BleError> = mutex.withLock {
        connections[deviceUuid]?.let { return@withLock Outcome.Ok(it) }

        if (connections.size >= maxConnections) {
            val victim = connections.keys.firstOrNull()
            if (victim != null) {
                connections.remove(victim)?.close()
            }
        }

        val connection = connectionFactory(deviceUuid, device)
        when (val result = connection.connectAndAuthenticate(username, password)) {
            is Outcome.Ok -> {
                connections[deviceUuid] = connection
                Outcome.Ok(connection)
            }
            is Outcome.Err -> {
                connection.close()
                Outcome.Err(result.error)
            }
        }
    }

    override fun get(deviceUuid: String): BleConnection? = connections[deviceUuid]

    override suspend fun close(deviceUuid: String) = mutex.withLock {
        connections.remove(deviceUuid)?.close()
        Unit
    }

    override suspend fun closeAll() = mutex.withLock {
        connections.values.toList().forEach { it.close() }
        connections.clear()
    }

    companion object {
        const val DEFAULT_MAX_CONNECTIONS = 4
    }
}
