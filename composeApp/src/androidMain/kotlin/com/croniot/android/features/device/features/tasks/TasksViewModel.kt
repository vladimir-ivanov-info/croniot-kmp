package com.croniot.android.features.device.features.tasks

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// import com.croniot.android.app.Global
// import com.croniot.android.core.constants.ServerConfig
import com.croniot.client.core.models.Task
import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.TaskTypesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import com.croniot.client.core.models.TaskType
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.domain.usecases.ObserveNewTasksUseCase
import com.croniot.client.domain.usecases.ObserveTaskStateInfoUseCase
import Outcome

class TasksViewModel(
    private val localDataRepository: LocalDataRepository,
    private val tasksRepository: TasksRepository,
    private val fetchTasksUseCase: FetchTasksUseCase,
    private val observeNewTasksUseCase: ObserveNewTasksUseCase,
    private val observeTaskStateInfoUseCase: ObserveTaskStateInfoUseCase,
    private val taskTypesRepository: TaskTypesRepository,
) : ViewModel(), KoinComponent {

    private var _tasksLoaded = false
    private var _tasksBeingListened = false

    private var _newTasksBeingListened = false

    // TODO transform into a state that contains static TaskType and Task data and dynamic (mutableStateFlow) of task progress
    private val _tasks = MutableStateFlow<List<MutableStateFlow<Task>>>(emptyList())
    val tasks: StateFlow<List<StateFlow<Task>>> get() = _tasks

    // private val _tasks = MutableStateFlow(null)
    // val tasks: StateFlow<Task> get() = _tasks

    // var mqttClient: MqttClient? = null

   /* fun bindTaskStream(cold: Flow<Task>) {
        cold
            .distinctUntilChanged()    // si Task implementa equals correctamente
            .onEach { upsertTask(it) } // upsert por cada Task recibida
            .launchIn(viewModelScope)
    }*/

    /*private fun upsertTask(t: Task) {
        _tasks.update { current ->
            val byId = current.associateBy { it.value.uid }.toMutableMap()
            val flow = byId[t.uid]
            if (flow == null) {
                byId[t.uid] = MutableStateFlow(t)
            } else if (flow.value != t) {
                flow.value = t
            }
            byId.values.toList()
        }
    }*/

    private var selectedDeviceUuid: String? = null

    /*private data class TaskKey(val deviceUuid: String, val uid: Long)
    private val byKey = LinkedHashMap<TaskKey, MutableStateFlow<Task>>()

    private fun upsertTask(t: Task) {
        val key = TaskKey(t.deviceUuid, t.uid)
        val existing = byKey[key]
        if (existing == null) {
            val mf = MutableStateFlow(t)
            byKey[key] = mf
            // Solo si pertenece al device seleccionado, lo añadimos a la lista visible
            if (t.deviceUuid == selectedDeviceUuid) {
                _tasks.update { it + mf }
            }
        } else {
            // Si tu Task es data class, el equals va bien; si no, puedes asignar siempre
            if (existing.value != t) existing.value = t
        }
    }*/

    fun initialize(deviceUuid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!_tasksLoaded) {
                _tasksLoaded = true

                selectedDeviceUuid = deviceUuid
/*
                val resultFlow = fetchTasksUseCase(deviceUuid)

                    tasksRepository.observeNewTasks(deviceUuid)


                resultFlow.distinctUntilChanged()    // si Task implementa equals correctamente
                    .onEach { upsertTask(it) } // upsert por cada Task recibida
                    .launchIn(viewModelScope)
*/
                // bindTaskStream(resultFlow)

                // Si tu live depende de arrancar MQTT:
// tasksRepository.listenTasks(deviceUuid)

                val outcome = fetchTasksUseCase(deviceUuid)
                if (outcome is Outcome.Ok) {
                    _tasks.value += outcome.value.map { MutableStateFlow(it) }
                }

                observeNewTasksUseCase(deviceUuid).onEach { task ->
                    // println(it.uid)
                    _tasks.update { it + MutableStateFlow(task) }
                    // upsertTask(task)
                }.launchIn(viewModelScope)

                observeTaskStateInfoUseCase(deviceUuid).onEach { taskStateInfo ->
                    // println("${taskStateInfo.state}")
// 5215287 received


                    //TODO
                    /*val flow = _tasks.value.find { // 3452
                        it.value.uid == taskStateInfo.taskUid && it.value.deviceUuid == taskStateInfo.deviceUuid
                    }
                    flow?.update { current ->
                        current.copy(
                            stateInfos = current.stateInfos.toMutableSet().apply { add(taskStateInfo) },
                            // si usas lista:
                            // stateInfos = current.stateInfos.toMutableList().apply { add(si) }
                        )
                    }*/

                   /* val taskStateFlow = _tasks.value.find { it.value.uid == taskStateInfo.taskUid }
                    if(taskStateFlow != null){
                        taskStateFlow.update {
                            it.stateInfos.add(taskStateInfo)  //+= taskStateInfo
                            //stateInfos =
                        }
                    }*/
                }.launchIn(viewModelScope)
            }
        }
    }

    // helper para “upsert” en lista
    private fun List<Task>.upsertById(t: Task): List<Task> {
        val i = indexOfFirst { it.uid == t.uid && it.deviceUuid == t.deviceUuid }
        return if (i >= 0) toMutableList().apply { this[i] = t } else this + t
    }

    fun inititalize(deviceUuid: String) {
        // TODO
    }

    fun uninit() {
        _tasks.value = mutableStateListOf()

        _tasksLoaded = false
        _tasksBeingListened = false
        _newTasksBeingListened = false
    }


    fun listenToNewTasks() {
       /* viewModelScope.launch {
            if (!_newTasksBeingListened) {
                _newTasksBeingListened = true
                try {
                    val selectedDevice = localDataRepository.getSelectedDevice().first()

                    //val topic = "/${Global.selectedDevice?.uuid}/newTasks"
                    val topic = "/${selectedDevice?.uuid}/newTasks"
                    val mqttClient = MqttClient(
                        ServerConfig.mqttBrokerUrl,
                        ServerConfig.mqttClientId + Global.generateUniqueString(
                            8,
                        ),
                        null,
                    )

                    //val selectedDevice = Global.selectedDevice
                    selectedDevice?.let {
                        MqttHandler(mqttClient, MqttDataProcessorNewTask(it.uuid), topic)
                    }
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
        }*/
    }

    // fun updateTaskProgress(taskStateInfoDto: TaskStateInfoDto) {
    fun updateTaskProgress(taskStateInfoDto: TaskStateInfo) {
        //TODO
        /*val listOfMutableStateFlows = _tasks.value
        val taskMutableStateFlow = listOfMutableStateFlows.find { it.value.uid == taskStateInfoDto.taskUid }

        taskMutableStateFlow?.let {
            it.update { currentValue ->
                currentValue.copy(
                    stateInfos = currentValue.stateInfos.toMutableSet().apply {
                        add(taskStateInfoDto)
                    },
                )
            }
        }*/
    }

    /*fun addTask(taskDto: TaskDto) {
        _tasks.update { currentTasks ->
            val existingTask = currentTasks.find { it.value.uid == taskDto.uid }

            existingTask?.let {
                existingTask.value = taskDto // Update existing task
                currentTasks // Return unchanged list
            } ?: run {
                currentTasks + MutableStateFlow(taskDto) // Return new list with added task
            }
        }
    }*/

    fun getTaskType(deviceUuid: String, taskTypeUid: Long): TaskType? {
        return taskTypesRepository.get(deviceUuid, taskTypeUid)
    }
}
