package com.croniot.client.features.tasktypes.presentation.tasktypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.core.models.TaskType
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.domain.usecases.GetLatestTaskStateInfoUseCase
import com.croniot.client.domain.usecases.ObserveTaskStateInfoUseCase
import com.croniot.client.domain.usecases.RequestTaskStateInfoSyncUseCase
import com.croniot.client.presentation.util.formatStateInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskTypesViewModel(
    private val fetchTasksUseCase: FetchTasksUseCase,
    private val requestTaskStateInfoSyncUseCase: RequestTaskStateInfoSyncUseCase,
    private val observeTaskStateInfoUseCase: ObserveTaskStateInfoUseCase,
    private val getLatestTaskStateInfoUseCase: GetLatestTaskStateInfoUseCase,
) : ViewModel() {

    private val secondaryTextFlows = mutableMapOf<String, StateFlow<String>>()
    private val taskStateInfoFlows = mutableMapOf<String, StateFlow<TaskStateInfo?>>()

    fun initialize(deviceUuid: String, taskTypes: List<TaskType>) {
        viewModelScope.launch {
            fetchTasksUseCase(deviceUuid)
            taskTypes.forEach { taskType ->
                requestTaskStateInfoSyncUseCase(deviceUuid, taskType.uid)
                //observeTaskTypeUpdates(deviceUuid, taskType)
            }
        }
    }

    fun observeTaskTypeUpdates(deviceUuid: String, taskType: TaskType): StateFlow<TaskStateInfo?> {
        val key = "$deviceUuid|${taskType.uid}"
        return taskStateInfoFlows.getOrPut(key) {
            observeTaskStateInfoUseCase(deviceUuid, taskType.uid).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null,
            )
        }
    }

    fun getSecondaryText(deviceUuid: String, taskType: TaskType): StateFlow<String> {
        val key = "$deviceUuid|${taskType.uid}"
        return secondaryTextFlows.getOrPut(key) {
            val initial = getLatestTaskStateInfoUseCase(deviceUuid, taskType.uid)
                ?.let { formatStateInfo(it) } ?: ""
            observeTaskStateInfoUseCase(deviceUuid, taskType.uid)
                .map { info -> formatStateInfo(info) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
        }
    }

}
