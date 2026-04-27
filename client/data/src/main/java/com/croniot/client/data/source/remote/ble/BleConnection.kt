package com.croniot.client.data.source.remote.ble

import Outcome
import com.croniot.client.domain.errors.BleError
import com.croniot.client.domain.models.SensorData
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import croniot.messages.MessageAddTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BleConnection {
    val deviceUuid: String
    val connectionState: StateFlow<BleConnectionState>

    suspend fun connectAndAuthenticate(
        username: String,
        password: String,
    ): Outcome<Unit, BleError>

    fun observeSensorData(): Flow<SensorData>
    fun observeNewTasks(): Flow<Task>
    fun observeTaskStateInfoEvents(): Flow<TaskStateInfoEvent>

    suspend fun sendNewTask(message: MessageAddTask): Outcome<Unit, BleError>
    suspend fun requestTaskStateInfoSync(taskTypeUid: Long): Outcome<Unit, BleError>

    fun close()
}

enum class BleConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Authenticating,
    Ready,
    Failed,
}
