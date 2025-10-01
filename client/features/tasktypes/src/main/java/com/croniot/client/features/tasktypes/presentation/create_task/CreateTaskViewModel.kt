package com.croniot.client.features.tasktypes.presentation.create_task

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.core.models.TaskType
import com.croniot.client.core.models.isStateful
import com.croniot.client.data.repositories.TasksRepository
import com.croniot.client.data.repositories.LocalDataRepository
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.features.tasktypes.usecases.SendNewTaskUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import croniot.models.Result
import kotlinx.coroutines.flow.filter

class CreateTaskViewModel(
    private val localDataRepository: LocalDataRepository,
    private val tasksRepository: TasksRepository,
    private val sendNewTaskUseCase: SendNewTaskUseCase,
    private val fetchTasksUseCase: FetchTasksUseCase
) : ViewModel(), KoinComponent {

    private val _taskType = mutableStateOf<TaskType?>(null)
    val taskType: State<TaskType?> = _taskType

    var deviceUuid : String? = null
    val parametersValues = mutableMapOf<Long, String>()



    private val _events = MutableSharedFlow<CreateTaskUiEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events = _events // asSharedFlow() si prefieres exponerlo inmutable


    fun initialize(_deviceUuid: String, _taskTypeUid: Long){
        viewModelScope.launch {
            val account = localDataRepository.getCurrentAccount()

            if(account != null){
                val device = account.devices.find { it.uuid == _deviceUuid }
                if(device != null){
                    deviceUuid = device.uuid


                    val taskType = device.taskTypes.find { it.uid == _taskTypeUid}
                    if(taskType != null){
                        //taskTypeUid = taskType.uid
                        _taskType.value = taskType
                    }
                }

            }
        }
    }

    fun updateParameter(parameterTaskUid: Long, newValue: String){
        viewModelScope.launch {
            parametersValues[parameterTaskUid] = newValue
        }
    }

    fun observeTaskTypeLatestState(deviceUuid: String, taskType: TaskType) : StateFlow<TaskStateInfo?> {
        return fetchTasksUseCase.observeTaskStateInfoUpdates(deviceUuid)
            .filter { it.taskTypeUid == taskType.uid }
            .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = fetchTasksUseCase.getLatestTaskStateInfo(deviceUuid, taskType.uid)
        )
    }

    fun sendTask(){
        viewModelScope.launch {
            val finalDeviceUuid = deviceUuid
            val finalTaskTypeUid = taskType.value?.uid

            if(finalDeviceUuid != null && finalTaskTypeUid != null){
                val result = sendNewTaskUseCase(finalDeviceUuid, finalTaskTypeUid, parametersValues)

                //val isTaskStateful = _taskType.value?.parameters?.any { it.isStateful()  } ?: false

                //if(!isTaskStateful){ //If task is stateful, don't show info to user
                    _events.tryEmit(CreateTaskUiEvent.ShowSnackbar(result))
               // }
            }
        }
    }

    fun sendStatefulTask(deviceUuid: String, taskTypeUid: Long, parameterUid: Long, newValue: String) {
        viewModelScope.launch {
            val parametersValues = mutableMapOf<Long, String>()
            parametersValues[parameterUid] = newValue
            val result = sendNewTaskUseCase(deviceUuid,taskTypeUid, parametersValues)
            //_events.tryEmit(CreateTaskUiEvent.ShowSnackbar(result))
        }
    }
}

sealed interface CreateTaskUiEvent {
    data class ShowSnackbar(val result: Result) : CreateTaskUiEvent
}