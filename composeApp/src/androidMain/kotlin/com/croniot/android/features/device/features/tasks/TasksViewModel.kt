package com.croniot.android.features.device.features.tasks

import Outcome
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.core.models.TaskType
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.domain.usecases.ObserveNewTasksUseCase
import com.croniot.client.domain.usecases.ObserveTaskStateInfoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class TasksViewModel(
    private val localDataRepository: LocalDataRepository,
    private val tasksRepository: TasksRepository,
    private val fetchTasksUseCase: FetchTasksUseCase,
    private val observeNewTasksUseCase: ObserveNewTasksUseCase,
    private val observeTaskStateInfoUseCase: ObserveTaskStateInfoUseCase,
    private val taskTypesRepository: TaskTypesRepository,
) : ViewModel() {

    private val _tasksLoaded = AtomicBoolean(false)
    private var observeJob: Job? = null

    private val _tasks = MutableStateFlow<List<MutableStateFlow<Task>>>(emptyList())
    val tasks: StateFlow<List<StateFlow<Task>>> get() = _tasks

    private var selectedDeviceUuid: String? = null

    fun initialize(deviceUuid: String) {
        selectedDeviceUuid = deviceUuid

        if (_tasksLoaded.compareAndSet(false, true)) {
            viewModelScope.launch(Dispatchers.IO) {
                val outcome = fetchTasksUseCase(deviceUuid)
                if (outcome is Outcome.Ok) {
                    _tasks.value += outcome.value.map { MutableStateFlow(it) }
                }
            }
        }

        startObserving(deviceUuid)
    }

    private fun startObserving(deviceUuid: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch(Dispatchers.IO) {
            launch {
                observeNewTasksUseCase(deviceUuid).collect { task ->
                    _tasks.update { it + MutableStateFlow(task) }
                }
            }
            launch {
                observeTaskStateInfoUseCase(deviceUuid).collect { taskStateInfo ->
                    // TODO: update task state in _tasks
                }
            }
        }
    }

    fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
    }

    fun uninit() {
        _tasks.value = emptyList()
        _tasksLoaded.set(false)
    }

    fun updateTaskProgress(taskStateInfoDto: TaskStateInfo) {
        // TODO
    }

    fun getTaskType(deviceUuid: String, taskTypeUid: Long): TaskType? {
        return taskTypesRepository.get(deviceUuid, taskTypeUid)
    }
}