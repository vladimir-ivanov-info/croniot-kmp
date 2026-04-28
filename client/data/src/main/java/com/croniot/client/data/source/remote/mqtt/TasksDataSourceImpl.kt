package com.croniot.client.data.source.remote.mqtt

import MqttHandler
import Outcome
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.data.mappers.toModel
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import com.croniot.client.data.source.remote.http.TaskApi
import com.croniot.client.data.util.TaggingSocketFactory
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import com.croniot.client.core.util.StringUtil.generateUniqueString
import croniot.messages.MessageAddTask
import croniot.messages.MessageRequestTaskStateInfoSync
import croniot.models.MqttTopics
import flatMap
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import map
import org.eclipse.paho.client.mqttv3.MqttClient
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

class TasksDataSourceImpl(
    private val taskApi: TaskApi,
    private val localDatasource: ServerConfigLocalDatasource,
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
            mqttDataProcessor = MqttDataProcessorTaskStateInfo(onNewEvent),
            topic = topic,
            scope = appScope,
            socketFactory = TaggingSocketFactory(),
        )
        progressHandlers[deviceUuid] = handler
    }

    override suspend fun stopListening(deviceUuid: String) {
        newTaskHandlers.remove(deviceUuid)?.disconnect()
        progressHandlers.remove(deviceUuid)?.disconnect()
    }

    override suspend fun stopAllListeners() {
        newTaskHandlers.values.forEach { it.disconnect() }
        newTaskHandlers.clear()
        progressHandlers.values.forEach { it.disconnect() }
        progressHandlers.clear()
    }

    override suspend fun fetchTasks(deviceUuid: String): Outcome<List<Task>, TaskError> =
        runRemote { taskApi.requestTaskConfigurations(deviceUuid) }
            .map { dtos -> dtos.map { it.toModel() } }

    override suspend fun sendNewTask(messageAddTask: MessageAddTask): Outcome<Unit, TaskError> =
        runRemote { taskApi.addTask(messageAddTask) }
            .flatMap { result ->
                if (result.success) Outcome.Ok(Unit)
                else Outcome.Err(TaskError.Remote(RemoteError.ServerError(result.message)))
            }

    override suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeUid: Long): Outcome<Unit, TaskError> =
        runRemote {
            taskApi.requestTaskStateInfoSync(
                MessageRequestTaskStateInfoSync(deviceUuid, taskTypeUid.toString())
            )
        }.flatMap { result ->
            if (result.success) Outcome.Ok(Unit)
            else Outcome.Err(TaskError.Remote(RemoteError.ServerError(result.message)))
        }

    override suspend fun fetchTaskStateInfoHistory(
        deviceUuid: String,
        limit: Int,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Outcome<List<TaskStateInfoHistoryEntry>, TaskError> =
        runRemote {
            taskApi.requestTaskStateInfoHistory(deviceUuid, limit, before, beforeId, taskTypeUid)
        }.map { dtos -> dtos.map { it.toModel(deviceUuid) } }

    override suspend fun fetchTaskStateInfoHistoryCount(
        deviceUuid: String,
        before: String?,
        beforeId: Long?,
        taskTypeUid: Long?,
    ): Outcome<Int, TaskError> =
        runRemote { taskApi.requestTaskStateInfoHistoryCount(deviceUuid, before, beforeId, taskTypeUid) }

    private suspend inline fun <T> runRemote(block: () -> T): Outcome<T, TaskError> = try {
        Outcome.Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        Outcome.Err(TaskError.Remote(RemoteError.Http(e.response.status.value)))
    } catch (e: HttpRequestTimeoutException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: ConnectTimeoutException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: SocketTimeoutException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: IOException) {
        Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
    } catch (e: Exception) {
        android.util.Log.e("TasksDataSource", "remote call failed", e)
        Outcome.Err(TaskError.Remote(RemoteError.Unknown))
    }
}
