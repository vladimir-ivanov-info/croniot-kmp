package com.croniot.client.features.tasktypes.presentation.create_task

import Outcome
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.usecases.GetLatestTaskStateInfoUseCase
import com.croniot.client.domain.usecases.ObserveTaskStateInfoUseCase
import com.croniot.client.domain.usecases.SendNewTaskUseCase
import com.croniot.client.features.tasktypes.R
import com.croniot.client.presentation.UiText
import com.croniot.client.presentation.toUiText
import croniot.models.TaskState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class CreateTaskViewModel(
    private val localDataRepository: LocalDataRepository,
    private val sendNewTaskUseCase: SendNewTaskUseCase,
    private val observeTaskStateInfoUseCase: ObserveTaskStateInfoUseCase,
    private val getLatestTaskStateInfoUseCase: GetLatestTaskStateInfoUseCase,
) : ViewModel() {

    private val _taskType = MutableStateFlow<TaskType?>(null)
    val taskType: StateFlow<TaskType?> = _taskType.asStateFlow()

    private var deviceUuid: String? = null
    private val parametersValues = mutableMapOf<Long, String>()

    private val _events = MutableSharedFlow<CreateTaskUiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val events = _events.asSharedFlow()

    private var _latestStateFlow: StateFlow<TaskStateInfo?>? = null
    private var sendTimestampMs = 0L

    fun initialize(_deviceUuid: String, _taskTypeUid: Long) {
        if (deviceUuid != null) return
        viewModelScope.launch {
            val account = localDataRepository.getCurrentAccount()

            if (account != null) {
                val device = account.devices.find { it.uuid == _deviceUuid }
                if (device != null) {
                    deviceUuid = device.uuid

                    val taskType = device.taskTypes.find { it.uid == _taskTypeUid }
                    if (taskType != null) {
                        _taskType.value = taskType
                    }
                }
            }
        }
    }

    fun updateParameter(parameterTaskUid: Long, newValue: String) {
        parametersValues[parameterTaskUid] = newValue
    }

    fun observeTaskTypeLatestState(deviceUuid: String, taskType: TaskType): StateFlow<TaskStateInfo?> {
        return _latestStateFlow ?: observeTaskStateInfoUseCase(deviceUuid, taskType.uid)
            .onEach { state ->
                val isIoTState = state.state != TaskState.CREATED.name &&
                    state.state != TaskState.UNDEFINED.name &&
                    state.state != TaskState.ERROR.name
                if (sendTimestampMs > 0 && isIoTState) {
                    Log.d("RTT", "Round-trip: ${System.currentTimeMillis() - sendTimestampMs}ms → ${state.state}")
                    sendTimestampMs = 0
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = getLatestTaskStateInfoUseCase(deviceUuid, taskType.uid),
            )
            .also { _latestStateFlow = it }
    }

    fun sendTask() {
        viewModelScope.launch {
            val finalDeviceUuid = deviceUuid
            val finalTaskTypeUid = taskType.value?.uid

            if (finalDeviceUuid != null && finalTaskTypeUid != null) {
                val result = sendNewTaskUseCase(finalDeviceUuid, finalTaskTypeUid, parametersValues)
                _events.tryEmit(
                    CreateTaskUiEvent.ShowSnackbar(result.toUiText())
                )
            }
        }
    }

    fun sendStatefulTask(deviceUuid: String, taskTypeUid: Long, parameterUid: Long, newValue: String) {
        viewModelScope.launch {
            sendTimestampMs = System.currentTimeMillis()
            val params = mutableMapOf(parameterUid to newValue)
            val result = sendNewTaskUseCase(deviceUuid, taskTypeUid, params)
            if(result is Outcome.Err){
                _events.tryEmit(CreateTaskUiEvent.ShowSnackbar(result.toUiText()))
            }
        }
    }
}

sealed interface CreateTaskUiEvent {
    data class ShowSnackbar(val message: UiText) : CreateTaskUiEvent
}

private fun Outcome<Unit, TaskError>.toUiText(): UiText = when (this) {
    is Outcome.Ok -> UiText.Resource(R.string.task_sent_successfully)
    is Outcome.Err -> when (val taskError = error) {
        is TaskError.Remote -> taskError.error.toUiText()
    }
}
