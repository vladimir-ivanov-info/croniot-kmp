package com.croniot.client.data.source.transport

import com.croniot.client.domain.models.TransportKind
import kotlinx.coroutines.flow.StateFlow

interface TransportRouter {
    val bleDeviceUuids: StateFlow<Set<String>>
    fun transportFor(deviceUuid: String): TransportKind
    suspend fun markBle(deviceUuid: String)
    suspend fun markCloud(deviceUuid: String)
    suspend fun loadInitial()
}
