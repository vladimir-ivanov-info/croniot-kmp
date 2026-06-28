package com.croniot.client.data.source.transport

import com.croniot.client.data.source.local.database.daos.BleKnownDeviceDao
import com.croniot.client.domain.models.TransportKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TransportRouterImpl(
    private val bleKnownDeviceDao: BleKnownDeviceDao,
) : TransportRouter {

    private val writeMutex = Mutex()
    private val _bleDeviceUuids = MutableStateFlow<Set<String>>(emptySet())

    override val bleDeviceUuids: StateFlow<Set<String>> = _bleDeviceUuids.asStateFlow()

    override fun transportFor(deviceUuid: String): TransportKind =
        if (deviceUuid in _bleDeviceUuids.value) TransportKind.BLE else TransportKind.CLOUD

    override suspend fun markBle(deviceUuid: String) {
        writeMutex.withLock {
            _bleDeviceUuids.update { current -> current + deviceUuid }
        }
    }

    override suspend fun markCloud(deviceUuid: String) {
        writeMutex.withLock {
            _bleDeviceUuids.update { current -> current - deviceUuid }
        }
    }

    override suspend fun loadInitial() {
        val uuidsFromDb = bleKnownDeviceDao.getAllUuids().toSet()
        writeMutex.withLock {
            _bleDeviceUuids.value = uuidsFromDb
        }
    }
}
