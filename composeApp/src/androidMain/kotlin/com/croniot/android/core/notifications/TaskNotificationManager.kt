package com.croniot.android.core.notifications

import com.croniot.client.domain.models.events.TaskStateInfoEvent
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.TasksRepository
import croniot.models.TaskState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class TaskNotificationManager(
    private val notificationHelper: NotificationHelper,
    private val tasksRepository: TasksRepository,
    private val taskTypesRepository: TaskTypesRepository,
) {

    private data class NotifKey(val deviceUuid: String, val taskTypeUid: Long)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val notificationIdByType = ConcurrentHashMap<NotifKey, Int>()
    private val idCounter = AtomicInteger(2000)

    fun startObserving(deviceUuid: String) {
        if (activeJobs.containsKey(deviceUuid)) return

        val job = scope.launch {
            tasksRepository.observeTaskStateInfoUpdates(deviceUuid)
                .collect { event -> handleEvent(event) }
        }
        activeJobs[deviceUuid] = job
    }

    fun stopAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        notificationIdByType.values.forEach { notificationHelper.cancel(it) }
        notificationIdByType.clear()
    }

    private fun handleEvent(event: TaskStateInfoEvent) {
        val info = event.info
        val state = TaskState.fromString(info.state)
        val deviceUuid = event.key.deviceUuid
        val taskTypeUid = event.key.taskTypeUid

        val taskTypeName = taskTypesRepository.get(deviceUuid, taskTypeUid)?.name
            ?: "Task #$taskTypeUid"

        val notifKey = NotifKey(deviceUuid, taskTypeUid)
        val notifId = notificationIdByType.getOrPut(notifKey) { idCounter.getAndIncrement() }

        when (state) {
            TaskState.RUNNING -> {
                notificationHelper.showProgress(
                    title = taskTypeName,
                    text = "${info.progress.toInt()}%",
                    progress = info.progress.toInt(),
                    ongoing = true,
                    notificationId = notifId,
                )
            }

            TaskState.COMPLETED -> {
                notificationHelper.show(
                    title = taskTypeName,
                    text = "Completed",
                    channelId = NotificationHelper.CHANNEL_ID_TASK_PROGRESS,
                    notificationId = notifId,
                )
            }

            TaskState.ERROR -> {
                notificationHelper.show(
                    title = taskTypeName,
                    text = info.errorMessage.ifEmpty { "Error" },
                    channelId = NotificationHelper.CHANNEL_ID_TASK_PROGRESS,
                    notificationId = notifId,
                )
            }

            else -> { /* CREATED, RECEIVED, etc. — no notification */ }
        }
    }
}
