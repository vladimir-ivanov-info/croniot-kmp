package com.croniot.client.data.source.remote.mqtt

import MqttHandler
import Outcome
import com.croniot.client.core.config.Constants.ENDPOINT_ADD_TASK
import com.croniot.client.core.config.Constants.ENDPOINT_REQUEST_TASK_STATE_INFO_SYNC
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.core.mappers.toModel
import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfoHistoryEntry
import com.croniot.client.core.models.events.TaskStateInfoEvent
import com.croniot.client.core.util.StringUtil.generateUniqueString
import com.croniot.client.data.source.local.LocalDatasource
import com.croniot.client.data.source.remote.http.NetworkUtil
import com.croniot.client.data.source.remote.http.TaskConfigurationApiService
import croniot.models.MqttTopics
import com.croniot.client.data.util.TaggingSocketFactory
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import croniot.messages.MessageAddTask
import croniot.messages.MessageFactory
import croniot.messages.MessageRequestTaskStateInfoSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

class TasksDataSourceImpl(
    private val networkUtil: NetworkUtil,
    private val taskConfigurationApiService: TaskConfigurationApiService,
    private val localDatasource: LocalDatasource,
    private val appScope: CoroutineScope,
) : TasksDataSource {

    private val newTaskHandlers = ConcurrentHashMap<String, MqttHandler>()
    private val progressHandlers = ConcurrentHashMap<String, MqttHandler>()

    private suspend fun mqttBrokerUrl(): String {
        val ip = localDatasource.getServerIp().first() ?: ServerConfig.DEFAULT_MQTT_HOST
        return "tcp://${ip}:${ServerConfig.MQTT_PORT}"
    }

    override suspend fun listenTasks(
        deviceUuid: String,
        onNewTask: (Task) -> Unit,
    ) = withContext(Dispatchers.IO) {
        newTaskHandlers.remove(deviceUuid)?.disconnect()

        val clientId = ServerConfig.mqttClientId + generateUniqueString(8)
        val mqttClient = MqttClient(mqttBrokerUrl(), clientId, null)
        val topic = MqttTopics.newTasks(deviceUuid)

        val handler = MqttHandler(
            mqttClient = mqttClient,
            mqttDataProcessor = MqttDataProcessorNewTask(
                deviceUuid = deviceUuid,
                onNewTask = { task -> onNewTask(task.copy(deviceUuid = deviceUuid)) },
            ),
            topic = topic,
            scope = appScope,
            socketFactory = TaggingSocketFactory(),
        )
        newTaskHandlers[deviceUuid] = handler
    }

    override suspend fun listenTaskStateInfos(
        deviceUuid: String,
        onNewEvent: (TaskStateInfoEvent) -> Unit,
    ) = withContext(Dispatchers.IO) {
        progressHandlers.remove(deviceUuid)?.disconnect()

        val clientId = ServerConfig.mqttClientId + generateUniqueString(8)
        val mqttClient = MqttClient(mqttBrokerUrl(), clientId, null)
        val topic = MqttTopics.taskProgressWildcard(deviceUuid)

        android.util.Log.d("RTT", "Subscribing progress: topic=$topic clientId=$clientId")

        val handler = MqttHandler(
            mqttClient = mqttClient,
            mqttDataProcessor = MqttDataProcessorTaskProgress(onNewEvent),
            topic = topic,
            scope = appScope,
            socketFactory = TaggingSocketFactory(),
        )
        progressHandlers[deviceUuid] = handler
    }

    override suspend fun stopAllListeners() {
        newTaskHandlers.values.forEach { it.disconnect() }
        newTaskHandlers.clear()
        progressHandlers.values.forEach { it.disconnect() }
        progressHandlers.clear()
    }

    override suspend fun fetchTasks(deviceUuid: String): Outcome<List<Task>, TaskError> = try {
        val response = taskConfigurationApiService.requestTaskConfigurations(deviceUuid)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Outcome.Ok(body.map { it.toModel() })
        } else {
            Outcome.Err(TaskError.Remote(RemoteError.Http(response.code())))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: Exception) {
        Outcome.Err(TaskError.Remote(RemoteError.Unknown))
    }

    override suspend fun sendNewTask(messageAddTask: MessageAddTask): Outcome<Unit, TaskError> = try {
        val result = networkUtil.post(ENDPOINT_ADD_TASK, MessageFactory.toJson(messageAddTask))
        if (result.success) {
            Outcome.Ok(Unit)
        } else {
            Outcome.Err(TaskError.Remote(RemoteError.ServerError(result.message)))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: Exception) {
        Outcome.Err(TaskError.Remote(RemoteError.Unknown))
    }

    override suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Outcome<Unit, TaskError> = try {
        val message = MessageRequestTaskStateInfoSync(deviceUuid, taskTypeUid.toString())
        val result = networkUtil.post(ENDPOINT_REQUEST_TASK_STATE_INFO_SYNC, MessageFactory.toJson(message))
        if (result.success) {
            Outcome.Ok(Unit)
        } else {
            Outcome.Err(TaskError.Remote(RemoteError.ServerError(result.message)))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: Exception) {
        Outcome.Err(TaskError.Remote(RemoteError.Unknown))
    }

    override suspend fun fetchTaskStateInfoHistory(deviceUuid: String): Outcome<List<TaskStateInfoHistoryEntry>, TaskError> = try {
        val response = taskConfigurationApiService.requestTaskStateInfoHistory(deviceUuid)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Outcome.Ok(body.map { it.toModel() })
        } else {
            Outcome.Err(TaskError.Remote(RemoteError.Http(response.code())))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: Exception) {
        Outcome.Err(TaskError.Remote(RemoteError.Unknown))
    }
}