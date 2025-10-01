package com.croniot.client.data.source.remote.mqtt

import MqttHandler
import com.croniot.client.core.Global
import com.croniot.client.core.ServerConfig
import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.core.models.mappers.toModel
import com.croniot.client.data.source.remote.TasksDataSource
import com.croniot.client.data.source.remote.http.RetrofitClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import org.eclipse.paho.client.mqttv3.MqttClient

class TasksDataSourceImpl() : TasksDataSource {

    override fun observeTasks(_deviceUuid: String): Flow<Task> = callbackFlow {
        val topic = "/$_deviceUuid/newTasks"
        val mqttClient = MqttClient(
            ServerConfig.mqttBrokerUrl,
            ServerConfig.mqttClientId + Global.generateUniqueString(8),
            null,
        )

        val handler = MqttHandler(
            mqttClient = mqttClient,
            mqttDataProcessor = MqttDataProcessorNewTask(
                deviceUuid = _deviceUuid,
                onNewTask = { task ->

                    val finalTask = task.copy(
                        deviceUuid = _deviceUuid,
                    )

                    val ok = trySend(finalTask).isSuccess
                    if (!ok) {
                        // log opcional: el canal está cerrado o lleno.
                    }
                },
            ),
            topic = topic,
        )

        // Mantén vivo el flow y cierra recursos al cancelar la colección
        awaitClose {
            runCatching { mqttClient.unsubscribe(topic) }
            runCatching { mqttClient.disconnect() }
            runCatching { mqttClient.close() }
            // si tu MqttHandler tiene stop(): runCatching { handler.stop() }
        }
    }.buffer(Channel.BUFFERED) // evita perder ráfagas

    override suspend fun fetchTasks(deviceUuid: String): List<Task> {
        // var tasksFlow : Flow<Task> = flowOf()
        var tasks = emptyList<Task>()
        try {
            val response =
                RetrofitClient.taskConfigurationApiService.requestTaskConfigurations(
                    deviceUuid,
                )
            val taskDtos = response.body()
            if (response.isSuccessful && taskDtos != null) {
                tasks = taskDtos.map { it.toModel() } // .asFlow()
            } else {
                println("Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }

        return tasks
    }

    override fun observeTaskStateInfos(_deviceUuid: String): Flow<TaskStateInfo> = callbackFlow {
        val clientId = ServerConfig.mqttClientId + Global.generateUniqueString(8)
        val mqttClient = MqttClient(ServerConfig.mqttBrokerUrl, clientId, null)

        val topic = "/server_to_devices/task_progress_update/$_deviceUuid"

        MqttHandler(
            mqttClient,
            MqttDataProcessorTaskProgress(onNewData = { newTaskStateInfo ->

                val finalNewTaskStateInfo = newTaskStateInfo.copy(
                    deviceUuid = _deviceUuid,
                )

                trySend(finalNewTaskStateInfo).isSuccess
            }),
            topic,
        )

        awaitClose {
            runCatching { mqttClient.unsubscribe(topic) }
            runCatching { mqttClient.disconnect() }
            runCatching { mqttClient.close() }
            // handler.stop() si procede
        }
    }.buffer(Channel.BUFFERED)
}
