package com.croniot.client.features.taskhistory.presentation

import Outcome
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

data class TaskHistoryItem(
    val taskUid: Long,
    val taskTypeUid: Long,
    val taskTypeName: String,
    val dateTime: ZonedDateTime,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)

sealed interface TaskHistoryState {
    data object Loading : TaskHistoryState
    data class Content(val entries: List<TaskHistoryItem>) : TaskHistoryState
}

class TaskHistoryViewModel(
    private val fetchTaskStateInfoHistoryUseCase: FetchTaskStateInfoHistoryUseCase,
    private val tasksRepository: TasksRepository,
    private val taskTypesRepository: TaskTypesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TaskHistoryState>(TaskHistoryState.Loading)
    val state: StateFlow<TaskHistoryState> = _state.asStateFlow()

    private val entries = CopyOnWriteArrayList<TaskHistoryItem>()
    private val initialized = AtomicBoolean(false)

    fun initialize(deviceUuid: String) {
        if (!initialized.compareAndSet(false, true)) return

        viewModelScope.launch {
            val outcome = fetchTaskStateInfoHistoryUseCase(deviceUuid)
            if (outcome is Outcome.Ok) {
                entries.addAll(outcome.value.map { it.toHistoryItem(deviceUuid) })
            }
            emitSorted()

            launch {
                tasksRepository.observeTaskStateInfoUpdates(deviceUuid).collect { event ->
                    val typeName = taskTypesRepository.get(deviceUuid, event.key.taskTypeUid)?.name ?: "Unknown"
                    val newEntry = TaskHistoryItem(
                        taskUid = event.key.taskUid,
                        taskTypeUid = event.key.taskTypeUid,
                        taskTypeName = typeName,
                        dateTime = event.info.dateTime,
                        state = event.info.state,
                        progress = event.info.progress,
                        errorMessage = event.info.errorMessage,
                    )
                    entries.add(0, newEntry)
                    emitSorted()
                }
            }
        }
    }

    private fun emitSorted() {
        val sorted = entries.sortedByDescending { it.dateTime }
        _state.update { TaskHistoryState.Content(sorted) }
    }

    private fun TaskStateInfoHistoryEntry.toHistoryItem(deviceUuid: String): TaskHistoryItem {
        val typeName = taskTypesRepository.get(deviceUuid, taskTypeUid)?.name ?: "Unknown"
        return TaskHistoryItem(
            taskUid = taskUid,
            taskTypeUid = taskTypeUid,
            taskTypeName = typeName,
            dateTime = dateTime,
            state = state,
            progress = progress,
            errorMessage = errorMessage,
        )
    }
}
