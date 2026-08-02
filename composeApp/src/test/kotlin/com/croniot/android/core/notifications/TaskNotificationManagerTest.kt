package com.croniot.android.core.notifications

import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import com.croniot.testing.fakes.FakeTaskTypesRepository
import com.croniot.testing.fakes.FakeTasksRepository
import croniot.models.TaskKey
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TaskNotificationManagerTest {

    private val notificationHelper: NotificationHelper = mockk(relaxed = true)
    private val taskTypesRepository = FakeTaskTypesRepository()
    private val deviceUuid = "device-1"

    private fun buildManager(events: MutableSharedFlow<TaskStateInfoEvent>): TaskNotificationManager {
        val tasksRepository = FakeTasksRepository(taskStateInfoEventsFlow = events)
        return TaskNotificationManager(notificationHelper, tasksRepository, taskTypesRepository)
    }

    private fun event(state: String, progress: Double = 0.0, errorMessage: String = "", taskTypeUid: Long = 10L) =
        TaskStateInfoEvent(
            key = TaskKey(deviceUuid, taskTypeUid, 1L),
            info = TaskStateInfo(dateTime = ZonedDateTime.now(), state = state, progress = progress, errorMessage = errorMessage),
        )

    // TaskNotificationManager.startObserving collects on a real Dispatchers.Default coroutine, launched
    // asynchronously. Emitting into the MutableSharedFlow immediately after startObserving() is a race:
    // the collector may not have subscribed yet, and a replay=0 SharedFlow drops emissions with no
    // subscriber. Waiting for subscriptionCount > 0 makes the emission deterministic.
    private fun emitAfterSubscribed(events: MutableSharedFlow<TaskStateInfoEvent>, value: TaskStateInfoEvent) {
        val deadline = System.currentTimeMillis() + 2000
        while (events.subscriptionCount.value == 0) {
            if (System.currentTimeMillis() > deadline) error("No subscriber attached in time")
            Thread.sleep(5)
        }
        events.tryEmit(value)
    }

    @Test
    fun `WHEN a RUNNING event is received THEN it shows a progress notification with the rounded percentage`() {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 4)
        val manager = buildManager(events)

        manager.startObserving(deviceUuid)
        emitAfterSubscribed(events, event(state = "RUNNING", progress = 42.0))

        verify(timeout = 2000) { notificationHelper.showProgress(title = any(), text = "42%", progress = 42, ongoing = true, notificationId = any()) }
    }

    @Test
    fun `WHEN a COMPLETED event is received THEN it shows a Completed notification`() {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 4)
        val manager = buildManager(events)

        manager.startObserving(deviceUuid)
        emitAfterSubscribed(events, event(state = "COMPLETED"))

        verify(timeout = 2000) { notificationHelper.show(title = any(), text = "Completed", channelId = NotificationHelper.CHANNEL_ID_TASK_PROGRESS, notificationId = any()) }
    }

    @Test
    fun `WHEN an ERROR event has a message THEN it shows that message`() {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 4)
        val manager = buildManager(events)

        manager.startObserving(deviceUuid)
        emitAfterSubscribed(events, event(state = "ERROR", errorMessage = "Sensor disconnected"))

        verify(timeout = 2000) { notificationHelper.show(title = any(), text = "Sensor disconnected", channelId = NotificationHelper.CHANNEL_ID_TASK_PROGRESS, notificationId = any()) }
    }

    @Test
    fun `WHEN an ERROR event has an empty message THEN it falls back to generic Error text`() {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 4)
        val manager = buildManager(events)

        manager.startObserving(deviceUuid)
        emitAfterSubscribed(events, event(state = "ERROR", errorMessage = ""))

        verify(timeout = 2000) { notificationHelper.show(title = any(), text = "Error", channelId = NotificationHelper.CHANNEL_ID_TASK_PROGRESS, notificationId = any()) }
    }

    @Test
    fun `WHEN a CREATED event is received THEN no notification is shown`() {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 4)
        val manager = buildManager(events)

        manager.startObserving(deviceUuid)
        emitAfterSubscribed(events, event(state = "CREATED"))
        Thread.sleep(300)

        verify(exactly = 0) { notificationHelper.show(any(), any(), any(), any()) }
        verify(exactly = 0) { notificationHelper.showProgress(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `WHEN the task type is registered THEN it uses the registered task type name`() {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 4)
        val taskType = TaskType(uid = 10L, name = "Watering", description = "", parameters = emptyList())
        taskTypesRepository.add(deviceUuid, taskType)
        val manager = buildManager(events)

        manager.startObserving(deviceUuid)
        emitAfterSubscribed(events, event(state = "COMPLETED", taskTypeUid = 10L))

        verify(timeout = 2000) { notificationHelper.show(title = "Watering", text = any(), channelId = any(), notificationId = any()) }
    }

    @Test
    fun `WHEN the task type is not registered THEN it falls back to Task hash uid`() {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 4)
        val manager = buildManager(events)

        manager.startObserving(deviceUuid)
        emitAfterSubscribed(events, event(state = "COMPLETED", taskTypeUid = 99L))

        verify(timeout = 2000) { notificationHelper.show(title = "Task #99", text = any(), channelId = any(), notificationId = any()) }
    }

    @Test
    fun `WHEN startObserving is called twice for the same device THEN it only subscribes once`() {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 4)
        val manager = buildManager(events)

        manager.startObserving(deviceUuid)
        manager.startObserving(deviceUuid)
        emitAfterSubscribed(events, event(state = "COMPLETED"))

        verify(timeout = 2000, exactly = 1) { notificationHelper.show(any(), any(), any(), any()) }
    }

    @Test
    fun `WHEN stopAll is called THEN it cancels active notifications`() {
        val events = MutableSharedFlow<TaskStateInfoEvent>(extraBufferCapacity = 4)
        val manager = buildManager(events)
        manager.startObserving(deviceUuid)
        emitAfterSubscribed(events, event(state = "COMPLETED"))
        verify(timeout = 2000) { notificationHelper.show(any(), any(), any(), any()) }

        manager.stopAll()

        verify(timeout = 2000) { notificationHelper.cancel(any()) }
    }
}
