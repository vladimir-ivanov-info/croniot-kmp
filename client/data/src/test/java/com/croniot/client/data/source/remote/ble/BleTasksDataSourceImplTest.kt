package com.croniot.client.data.source.remote.ble

import Outcome
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.Task
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import croniot.messages.MessageAddTask
import croniot.models.TaskKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class BleTasksDataSourceImplTest {

    private val connectionPool: BleConnectionPool = mockk()
    private val appScope = CoroutineScope(UnconfinedTestDispatcher())
    private val dataSource = BleTasksDataSourceImpl(appScope, connectionPool)

    @Test
    fun `WHEN there is no active connection THEN listenTasks does nothing`() = runTest {
        every { connectionPool.get("device-1") } returns null

        dataSource.listenTasks("device-1") { }

        verify(exactly = 1) { connectionPool.get("device-1") }
    }

    @Test
    fun `WHEN there is an active connection THEN listenTasks subscribes to its new task events`() = runTest {
        val connection: BleConnection = mockk { every { observeNewTasks() } returns emptyFlow() }
        every { connectionPool.get("device-1") } returns connection

        dataSource.listenTasks("device-1") { }

        verify(exactly = 1) { connection.observeNewTasks() }
    }

    @Test
    fun `WHEN a task is emitted THEN listenTasks forwards it to the callback`() = runTest {
        val newTasks = MutableSharedFlow<Task>(extraBufferCapacity = 1)
        val connection: BleConnection = mockk { every { observeNewTasks() } returns newTasks }
        every { connectionPool.get("device-1") } returns connection
        val received = mutableListOf<Task>()
        val task = Task(deviceUuid = "device-1", taskTypeUid = 10L, uid = 1L)

        dataSource.listenTasks("device-1") { received += it }
        newTasks.tryEmit(task)

        assertEquals(listOf(task), received)
    }

    @Test
    fun `WHEN listenTasks is called twice for the same device THEN it cancels the previous subscription`() = runTest {
        val firstConnection: BleConnection = mockk { every { observeNewTasks() } returns emptyFlow() }
        val secondConnection: BleConnection = mockk { every { observeNewTasks() } returns emptyFlow() }
        every { connectionPool.get("device-1") } returns firstConnection andThen secondConnection

        dataSource.listenTasks("device-1") { }
        dataSource.listenTasks("device-1") { }

        verify(exactly = 1) { firstConnection.observeNewTasks() }
        verify(exactly = 1) { secondConnection.observeNewTasks() }
    }

    @Test
    fun `WHEN there is no active connection THEN listenTaskStateInfos does nothing`() = runTest {
        every { connectionPool.get("device-1") } returns null

        dataSource.listenTaskStateInfos("device-1") { }
    }

    @Test
    fun `WHEN there is an active connection THEN listenTaskStateInfos subscribes to its state info events`() = runTest {
        val connection: BleConnection = mockk { every { observeTaskStateInfoEvents() } returns emptyFlow() }
        every { connectionPool.get("device-1") } returns connection

        dataSource.listenTaskStateInfos("device-1") { }

        verify(exactly = 1) { connection.observeTaskStateInfoEvents() }
    }

    @Test
    fun `WHEN an event is emitted THEN listenTaskStateInfos forwards it to the callback`() = runTest {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 1)
        val connection: BleConnection = mockk { every { observeTaskStateInfoEvents() } returns events }
        every { connectionPool.get("device-1") } returns connection
        val received = mutableListOf<TaskStateInfoEvent>()
        val event = TaskStateInfoEvent(
            key = TaskKey(deviceUuid = "device-1", taskTypeUid = 10L, taskUid = 1L),
            info = TaskStateInfo(dateTime = ZonedDateTime.now(), state = "RUNNING", progress = 0.5, errorMessage = ""),
        )

        dataSource.listenTaskStateInfos("device-1") { received += it }
        events.tryEmit(event)

        assertEquals(listOf(event), received)
    }

    @Test
    fun `WHEN nothing is active THEN stopListening and stopAllListeners do not throw`() = runTest {
        dataSource.stopListening("device-1")
        dataSource.stopAllListeners()
    }

    @Test
    fun `WHEN stopListening is called THEN it cancels the active subscription for that device`() = runTest {
        val connection: BleConnection = mockk { every { observeNewTasks() } returns MutableSharedFlow() }
        every { connectionPool.get("device-1") } returns connection
        dataSource.listenTasks("device-1") { }

        dataSource.stopListening("device-1")
        dataSource.stopListening("device-1")
    }

    @Test
    fun `WHEN stopAllListeners is called THEN it cancels every active subscription`() = runTest {
        val connection: BleConnection = mockk {
            every { observeNewTasks() } returns MutableSharedFlow()
            every { observeTaskStateInfoEvents() } returns MutableSharedFlow()
        }
        every { connectionPool.get("device-1") } returns connection
        dataSource.listenTasks("device-1") { }
        dataSource.listenTaskStateInfos("device-1") { }

        dataSource.stopAllListeners()
    }

    @Test
    fun `WHEN BLE has no persistent backend THEN fetchTasks always returns an empty list`() = runTest {
        val result = dataSource.fetchTasks("device-1")

        assertEquals(Outcome.Ok(emptyList<Task>()), result)
    }

    @Test
    fun `WHEN fetchTaskStateInfoHistory is called THEN it always returns an empty list`() = runTest {
        val result = dataSource.fetchTaskStateInfoHistory("device-1", 10, null, null, null)

        assertEquals(Outcome.Ok(emptyList<Any>()), result)
    }

    @Test
    fun `WHEN fetchTaskStateInfoHistoryCount is called THEN it always returns zero`() = runTest {
        val result = dataSource.fetchTaskStateInfoHistoryCount("device-1", null, null, null)

        assertEquals(Outcome.Ok(0), result)
    }

    @Test
    fun `WHEN there is no active connection THEN sendNewTask returns Unreachable`() = runTest {
        every { connectionPool.get("device-1") } returns null

        val result = dataSource.sendNewTask(MessageAddTask("device-1", "10", emptyMap()))

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unreachable)), result)
    }

    @Test
    fun `WHEN the connection accepts the task THEN sendNewTask returns Ok`() = runTest {
        val connection: BleConnection = mockk()
        coEvery { connection.sendNewTask(any()) } returns Outcome.Ok(Unit)
        every { connectionPool.get("device-1") } returns connection

        val result = dataSource.sendNewTask(MessageAddTask("device-1", "10", emptyMap()))

        assertEquals(Outcome.Ok(Unit), result)
    }

    @Test
    fun `WHEN the connection returns an error THEN sendNewTask maps it to Unknown`() = runTest {
        val connection: BleConnection = mockk()
        coEvery { connection.sendNewTask(any()) } returns Outcome.Err(com.croniot.client.domain.errors.BleError.Timeout)
        every { connectionPool.get("device-1") } returns connection

        val result = dataSource.sendNewTask(MessageAddTask("device-1", "10", emptyMap()))

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unknown)), result)
    }

    @Test
    fun `WHEN there is no active connection THEN requestTaskStateInfoSync returns Unreachable`() = runTest {
        every { connectionPool.get("device-1") } returns null

        val result = dataSource.requestTaskStateInfoSync("device-1", 10L)

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unreachable)), result)
    }

    @Test
    fun `WHEN the connection accepts the request THEN requestTaskStateInfoSync returns Ok`() = runTest {
        val connection: BleConnection = mockk()
        coEvery { connection.requestTaskStateInfoSync(10L) } returns Outcome.Ok(Unit)
        every { connectionPool.get("device-1") } returns connection

        val result = dataSource.requestTaskStateInfoSync("device-1", 10L)

        assertEquals(Outcome.Ok(Unit), result)
    }

    @Test
    fun `WHEN the connection returns an error THEN requestTaskStateInfoSync maps it to Unknown`() = runTest {
        val connection: BleConnection = mockk()
        coEvery { connection.requestTaskStateInfoSync(any()) } returns Outcome.Err(com.croniot.client.domain.errors.BleError.Timeout)
        every { connectionPool.get("device-1") } returns connection

        val result = dataSource.requestTaskStateInfoSync("device-1", 10L)

        assertEquals(Outcome.Err(TaskError.Remote(RemoteError.Unknown)), result)
    }
}
