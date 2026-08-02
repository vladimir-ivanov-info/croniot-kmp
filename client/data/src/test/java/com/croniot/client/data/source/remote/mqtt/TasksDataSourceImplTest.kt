package com.croniot.client.data.source.remote.mqtt

import MqttHandler
import Outcome
import android.os.StrictMode
import android.util.Log
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import com.croniot.client.data.source.remote.http.TaskApi
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.Task
import croniot.messages.MessageAddTask
import croniot.messages.MessageFactory
import croniot.messages.MessageRequestTaskStateInfoSync
import croniot.models.Result
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.ZonedDateTime

class TasksDataSourceImplTest {

    private val taskApi: TaskApi = mockk()
    private val localDatasource: ServerConfigLocalDatasource = mockk()
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var dataSource: TasksDataSourceImpl

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        dataSource = TasksDataSourceImpl(taskApi, localDatasource, appScope)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkStatic(StrictMode::class)
        unmockkConstructor(MqttClient::class, MqttHandler::class)
    }

    @Test
    fun `WHEN fetchTasks succeeds THEN it maps the dto list to domain tasks`() = runTest {
        coEvery { taskApi.requestTaskConfigurations("device-1") } returns listOf(TaskDto(uid = 1L, taskTypeUid = 10L))

        val result = dataSource.fetchTasks("device-1")

        assertTrue(result is Outcome.Ok)
        assertEquals(1, (result as Outcome.Ok).value.size)
        assertEquals(10L, result.value.first().taskTypeUid)
    }

    @Test
    fun `WHEN api throws IOException THEN fetchTasks maps it to Unreachable`() = runTest {
        coEvery { taskApi.requestTaskConfigurations(any()) } throws IOException("connection reset")

        val result = dataSource.fetchTasks("device-1")

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unreachable)), result)
    }

    @Test
    fun `WHEN api throws a generic exception THEN fetchTasks maps it to Unknown`() = runTest {
        coEvery { taskApi.requestTaskConfigurations(any()) } throws IllegalStateException("boom")

        val result = dataSource.fetchTasks("device-1")

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unknown)), result)
    }

    @Test
    fun `WHEN api throws SocketTimeoutException THEN fetchTasks maps it to Unreachable`() = runTest {
        coEvery { taskApi.requestTaskConfigurations(any()) } throws SocketTimeoutException("device-1", cause = IOException())

        val result = dataSource.fetchTasks("device-1")

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unreachable)), result)
    }

    @Test
    fun `WHEN api throws ConnectTimeoutException THEN fetchTasks maps it to Unreachable`() = runTest {
        coEvery { taskApi.requestTaskConfigurations(any()) } throws ConnectTimeoutException("device-1", cause = IOException())

        val result = dataSource.fetchTasks("device-1")

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unreachable)), result)
    }

    @Test
    fun `WHEN api throws HttpRequestTimeoutException THEN fetchTasks maps it to Unreachable`() = runTest {
        coEvery { taskApi.requestTaskConfigurations(any()) } throws HttpRequestTimeoutException("device-1", 5000)

        val result = dataSource.fetchTasks("device-1")

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unreachable)), result)
    }

    @Test
    fun `WHEN server reports success THEN sendNewTask returns Ok`() = runTest {
        coEvery { taskApi.addTask(any()) } returns Result(success = true)

        val result = dataSource.sendNewTask(MessageAddTask("device-1", "10", emptyMap()))

        assertEquals(Outcome.Ok(Unit), result)
    }

    @Test
    fun `WHEN server reports failure THEN sendNewTask returns ServerError`() = runTest {
        coEvery { taskApi.addTask(any()) } returns Result(success = false, message = "Invalid task")

        val result = dataSource.sendNewTask(MessageAddTask("device-1", "10", emptyMap()))

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.ServerError("Invalid task"))), result)
    }

    @Test
    fun `WHEN request succeeds THEN requestTaskStateInfoSync returns Ok`() = runTest {
        coEvery { taskApi.requestTaskStateInfoSync(any()) } returns Result(success = true)

        val result = dataSource.requestTaskStateInfoSync("device-1", 10L)

        assertEquals(Outcome.Ok(Unit), result)
    }

    @Test
    fun `WHEN requestTaskStateInfoSync is called THEN it passes taskTypeUid as string in the message`() = runTest {
        coEvery { taskApi.requestTaskStateInfoSync(any()) } returns Result(success = true)

        dataSource.requestTaskStateInfoSync("device-1", 42L)

        io.mockk.coVerify(exactly = 1) {
            taskApi.requestTaskStateInfoSync(MessageRequestTaskStateInfoSync("device-1", "42"))
        }
    }

    @Test
    fun `WHEN a business failure occurs THEN requestTaskStateInfoSync returns ServerError`() = runTest {
        coEvery { taskApi.requestTaskStateInfoSync(any()) } returns Result(success = false, message = "unknown type")

        val result = dataSource.requestTaskStateInfoSync("device-1", 10L)

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.ServerError("unknown type"))), result)
    }

    @Test
    fun `WHEN fetchTaskStateInfoHistory is called THEN it maps dtos to domain entries with deviceUuid`() = runTest {
        val dto = TaskStateInfoHistoryEntryDto(
            stateInfoId = 1L,
            taskUid = 5L,
            taskTypeUid = 10L,
            dateTime = ZonedDateTime.now(),
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        )
        coEvery {
            taskApi.requestTaskStateInfoHistory("device-1", 10, null, null, null)
        } returns listOf(dto)

        val result = dataSource.fetchTaskStateInfoHistory("device-1", 10, null, null, null)

        assertTrue(result is Outcome.Ok)
        val entries = (result as Outcome.Ok).value
        assertEquals("device-1", entries.first().taskKey.deviceUuid)
        assertEquals(5L, entries.first().taskKey.taskUid)
    }

    @Test
    fun `WHEN request succeeds THEN fetchTaskStateInfoHistoryCount returns the count`() = runTest {
        coEvery {
            taskApi.requestTaskStateInfoHistoryCount("device-1", null, null, null)
        } returns 7

        val result = dataSource.fetchTaskStateInfoHistoryCount("device-1", null, null, null)

        assertEquals(Outcome.Ok(7), result)
    }

    @Test
    fun `WHEN api throws an exception THEN fetchTaskStateInfoHistoryCount maps it to Unknown error`() = runTest {
        coEvery {
            taskApi.requestTaskStateInfoHistoryCount(any(), any(), any(), any())
        } throws RuntimeException("db error")

        val result = dataSource.fetchTaskStateInfoHistoryCount("device-1", null, null, null)

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unknown)), result)
    }

    @Test
    fun `WHEN api throws ResponseException THEN fetchTasks maps it to Http error carrying the status code`() = runTest {
        val response: HttpResponse = mockk { every { status } returns HttpStatusCode.NotFound }
        coEvery { taskApi.requestTaskConfigurations(any()) } throws ResponseException(response, "not found")

        val result = dataSource.fetchTasks("device-1")

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Http(404))), result)
    }

    @Test
    fun `WHEN there is no active listener for the device THEN stopListening is a no-op`() = runTest {
        dataSource.stopListening("device-1")
    }

    @Test
    fun `WHEN there are no active listeners THEN stopAllListeners is a no-op`() = runTest {
        dataSource.stopAllListeners()
    }

    // MqttHandler itself is out of scope (its init block opens a real MQTT connection), but
    // TasksDataSourceImpl's own branching around it (handler replacement/storage) is not, so
    // MqttClient/MqttHandler construction is stubbed out here to isolate that logic.
    private fun stubMqttConstruction() {
        mockkConstructor(MqttClient::class, MqttHandler::class)
        // mockkConstructor still runs the real constructor/init body; only explicitly stubbed
        // instance methods are overridden (unstubbed ones fall through to the real implementation).
        // MqttHandler's init block calls connect/setCallback/subscribe on the mqttClient it is
        // given, so those must be stubbed too or it performs a real network connection attempt.
        every { anyConstructed<MqttClient>().connect(any<MqttConnectOptions>()) } just Runs
        every { anyConstructed<MqttClient>().setCallback(any<MqttCallback>()) } just Runs
        every { anyConstructed<MqttClient>().subscribe(any<String>(), any<Int>()) } just Runs
        every { anyConstructed<MqttClient>().unsubscribe(any<String>()) } just Runs
        every { anyConstructed<MqttClient>().disconnect() } just Runs
        every { anyConstructed<MqttClient>().close() } just Runs
        every { anyConstructed<MqttHandler>().disconnect() } just Runs
        // TaggingSocketFactory()'s default-argument path calls the real android.os.StrictMode,
        // which is a stub on the JVM unit test classpath and throws unless mocked.
        mockkStatic(StrictMode::class)
        every { StrictMode.allowThreadDiskReads() } returns mockk(relaxed = true)
        every { StrictMode.setThreadPolicy(any()) } just Runs
    }

    @Test
    fun `WHEN listenTasks is called for the first time THEN it creates a handler and does not disconnect anything`() = runTest {
        coEvery { localDatasource.getServerIp() } returns flowOf("10.0.0.5")
        stubMqttConstruction()

        dataSource.listenTasks("device-1") { }

        verify(exactly = 0) { anyConstructed<MqttHandler>().disconnect() }
    }

    @Test
    fun `WHEN listenTasks is called again for the same device THEN it disconnects the previous handler`() = runTest {
        coEvery { localDatasource.getServerIp() } returns flowOf("10.0.0.5")
        stubMqttConstruction()

        dataSource.listenTasks("device-1") { }
        dataSource.listenTasks("device-1") { }

        verify(exactly = 1) { anyConstructed<MqttHandler>().disconnect() }
    }

    @Test
    fun `WHEN no server ip is stored THEN listenTasks falls back to the default MQTT host`() = runTest {
        coEvery { localDatasource.getServerIp() } returns flowOf(null)
        stubMqttConstruction()

        dataSource.listenTasks("device-1") { }

        verify(exactly = 0) { anyConstructed<MqttHandler>().disconnect() }
    }

    @Test
    fun `WHEN listenTaskStateInfos is called for the first time THEN it creates a handler and does not disconnect anything`() = runTest {
        coEvery { localDatasource.getServerIp() } returns flowOf("10.0.0.5")
        stubMqttConstruction()

        dataSource.listenTaskStateInfos("device-1") { }

        verify(exactly = 0) { anyConstructed<MqttHandler>().disconnect() }
    }

    @Test
    fun `WHEN listenTaskStateInfos is called again for the same device THEN it disconnects the previous handler`() = runTest {
        coEvery { localDatasource.getServerIp() } returns flowOf("10.0.0.5")
        stubMqttConstruction()

        dataSource.listenTaskStateInfos("device-1") { }
        dataSource.listenTaskStateInfos("device-1") { }

        verify(exactly = 1) { anyConstructed<MqttHandler>().disconnect() }
    }

    @Test
    fun `WHEN listenTasks and listenTaskStateInfos are both called THEN they track handlers independently per map`() = runTest {
        coEvery { localDatasource.getServerIp() } returns flowOf("10.0.0.5")
        stubMqttConstruction()

        dataSource.listenTasks("device-1") { }
        dataSource.listenTaskStateInfos("device-1") { }

        // Each call created a handler in its own map, neither should have triggered a disconnect yet.
        verify(exactly = 0) { anyConstructed<MqttHandler>().disconnect() }

        dataSource.stopListening("device-1")

        verify(exactly = 2) { anyConstructed<MqttHandler>().disconnect() }
    }

    @Test
    fun `WHEN an mqtt message arrives THEN listenTasks forwards it as a new task with the device uuid overridden`() = runTest {
        coEvery { localDatasource.getServerIp() } returns flowOf("10.0.0.5")
        stubMqttConstruction()
        val callbackSlot = io.mockk.slot<MqttCallback>()
        every { anyConstructed<MqttClient>().setCallback(capture(callbackSlot)) } just Runs

        val received = mutableListOf<Task>()
        dataSource.listenTasks("device-1") { task -> received.add(task) }

        val taskJson = MessageFactory.toJson(TaskDto(uid = 1L, taskTypeUid = 10L))
        callbackSlot.captured.messageArrived("some/topic", MqttMessage(taskJson.toByteArray()))

        assertEquals(1, received.size)
        assertEquals("device-1", received.first().deviceUuid)
        assertEquals(10L, received.first().taskTypeUid)
    }

    @Test
    fun `WHEN parameters are omitted THEN fetchTaskStateInfoHistory uses default parameters`() = runTest {
        coEvery {
            taskApi.requestTaskStateInfoHistory("device-1", 10, null, null, null)
        } returns emptyList()

        val result = dataSource.fetchTaskStateInfoHistory("device-1", 10)

        assertEquals(Outcome.Ok(emptyList<com.croniot.client.domain.models.TaskStateInfoHistoryEntry>()), result)
    }

    @Test
    fun `WHEN parameters are omitted THEN fetchTaskStateInfoHistoryCount uses default parameters`() = runTest {
        coEvery {
            taskApi.requestTaskStateInfoHistoryCount("device-1", null, null, null)
        } returns 0

        val result = dataSource.fetchTaskStateInfoHistoryCount("device-1")

        assertEquals(Outcome.Ok(0), result)
    }
}
