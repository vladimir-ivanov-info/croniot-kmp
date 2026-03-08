package com.croniot.client.data.source.remote.mqtt

import MqttHandler
import com.croniot.client.core.config.Constants.ENDPOINT_ADD_TASK
import com.croniot.client.core.config.Constants.ENDPOINT_REQUEST_TASK_STATE_INFO_SYNC
import com.croniot.client.core.config.AppConfig
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.core.models.Task
import com.croniot.client.core.models.events.TaskStateInfoEvent
import com.croniot.client.core.mappers.toModel
import com.croniot.client.data.source.remote.http.NetworkUtil
import com.croniot.client.data.source.remote.http.TaskConfigurationApiService
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.google.gson.GsonBuilder
import croniot.messages.MessageRequestTaskStateInfoSync
import Outcome
import com.croniot.client.core.util.StringUtil.generateUniqueString
import java.io.IOException
import croniot.messages.MessageAddTask
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import org.eclipse.paho.client.mqttv3.MqttClient

class TasksDataSourceImpl(
    private val networkUtil: NetworkUtil,
    private val taskConfigurationApiService: TaskConfigurationApiService,
) : TasksDataSource {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    override fun observeTasks(_deviceUuid: String): Flow<Task> = callbackFlow {
        val topic = "/$_deviceUuid/newTasks"
        val mqttClient = MqttClient(
            ServerConfig.mqttBrokerUrl,
            ServerConfig.mqttClientId + generateUniqueString(8),
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

        awaitClose {
            runCatching { mqttClient.unsubscribe(topic) }
            runCatching { mqttClient.disconnect() }
            runCatching { mqttClient.close() }
            //runCatching { handler.stop() }
        }
    }.buffer(Channel.BUFFERED)

    override suspend fun fetchTasks(deviceUuid: String): Outcome<List<Task>, TaskError> = try {
        val response = taskConfigurationApiService.requestTaskConfigurations(deviceUuid)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Outcome.Ok(body.map { it.toModel() })
        } else {
            Outcome.Err(TaskError.Remote(RemoteError.Http(response.code())))
        }
    } catch (e: IOException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: Exception) {
        Outcome.Err(TaskError.Remote(RemoteError.Unknown))
    }

    override fun observeTaskStateInfos(deviceUuid: String): Flow<TaskStateInfoEvent> = callbackFlow {
        val clientId = ServerConfig.mqttClientId + generateUniqueString(8)
        val mqttClient = MqttClient(ServerConfig.mqttBrokerUrl, clientId, null)

        val topic = "/server_to_devices/$deviceUuid/task_types/+/tasks/+/progress"

        val handler = MqttHandler(
            mqttClient = mqttClient,
            mqttDataProcessor = MqttDataProcessorTaskProgress { event ->
                trySend(event).isSuccess
            },
            topic = topic,
        )

        awaitClose {
            runCatching { mqttClient.unsubscribe(topic) }
            runCatching { mqttClient.disconnect() }
            runCatching { mqttClient.close() }
            // runCatching { handler.stop() } si existe
        }
    }.buffer(Channel.BUFFERED)


    override suspend fun sendNewTask(messageAddTask: MessageAddTask): Outcome<Unit, TaskError> = try {
        val result = networkUtil.post(ENDPOINT_ADD_TASK, gson.toJson(messageAddTask))
        if (result.success) Outcome.Ok(Unit)
        else Outcome.Err(TaskError.Remote(RemoteError.ServerError(result.message)))
    } catch (e: IOException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: Exception) {
        Outcome.Err(TaskError.Remote(RemoteError.Unknown))
    }

    override suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Outcome<Unit, TaskError> = try {
        val message = MessageRequestTaskStateInfoSync(deviceUuid, taskTypeUid.toString())
        val result = networkUtil.post(ENDPOINT_REQUEST_TASK_STATE_INFO_SYNC, gson.toJson(message))
        if (result.success) Outcome.Ok(Unit)
        else Outcome.Err(TaskError.Remote(RemoteError.ServerError(result.message)))
    } catch (e: IOException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: Exception) {
        Outcome.Err(TaskError.Remote(RemoteError.Unknown))
    }
}
