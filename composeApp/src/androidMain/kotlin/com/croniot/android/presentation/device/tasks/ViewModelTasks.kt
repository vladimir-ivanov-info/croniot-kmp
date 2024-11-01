package com.croniot.android.ui.task

import MqttHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.Global
import com.croniot.android.data.source.remote.retrofit.RetrofitClient
import com.croniot.android.presentation.device.tasks.MqttDataProcessorNewTask
import com.croniot.android.presentation.device.tasks.MqttDataProcessorTaskProgress
import croniot.models.TaskProgressUpdate
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.koin.core.component.KoinComponent
/*
class ViewModelTasks : ViewModel(), KoinComponent {

    private var _tasksLoaded = false
    private var _tasksBeingListened = false

    private val taskFlows = mutableMapOf<Long, MutableStateFlow<TaskDto>>()
    // Function to observe a specific task by ID
    fun getTaskFlow(taskId: Long): StateFlow<TaskDto?> {
        return taskFlows[taskId] ?: MutableStateFlow(null) // Returns existing flow or a default flow if task doesn't exist
    }
    fun addOrUpdateTask(task: TaskDto) {
        // Update or create an individual task flow
        val taskFlow = taskFlows.getOrPut(task.uid) { MutableStateFlow(task) }
        taskFlow.value = task // Update specific task's flow

        // Update the overall tasks list if it's a new task or needs reordering
        val updatedTasks = taskFlows.values.map { it.value }
        _tasks.value = updatedTasks
    }
    // Function to remove a task
    fun removeTask(taskId: Long) {
        taskFlows.remove(taskId)
        _tasks.value = taskFlows.values.map { it.value }
    }

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())
    val tasks: StateFlow<List<TaskDto>> get() = _tasks

    fun loadTasks(){
        //if(!_tasksLoaded){
            _tasksLoaded = true
            viewModelScope.launch {
                try {
                    val selectedDeviceUuid = Global.selectedDevice.uuid
                    val response = RetrofitClient.taskConfigurationApiService.requestTaskConfigurations(selectedDeviceUuid)
                    val tasksResponse = response.body()
                    if (response.isSuccessful && tasksResponse != null) {
                        for(t in tasksResponse){
                            addOrUpdateTask(t)
                        }
                        _tasks.emit(tasksResponse)
                    } else {
                        println("Error: ${response.errorBody()?.string()}")
                    }


                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
        //}
    }

    fun listenToTasksUpdates(){
        if(!_tasksBeingListened){
            _tasksBeingListened = true
            try{

                    val topic =  "/iot_to_server/task_progress_update/${Global.selectedDevice.uuid}"
                    //   val mqttClient = MqttClient(Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(8))
                    var mqttClient = MqttClient(
                        Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(
                            8
                        ), null)
                    MqttHandler(mqttClient, MqttDataProcessorTaskProgress(Global.selectedDevice.uuid, 123), topic)

            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    fun updateTaskProgress(taskProgressUpdate: TaskProgressUpdate) {
        val task = taskFlows[taskProgressUpdate.taskUid]?.value
        if (task != null) {
            task.progress = taskProgressUpdate.progress

            // Emit a new list to trigger recomposition
            _tasks.value = taskFlows.values.map { it.value }
        }
    }

}*/

class ViewModelTasks : ViewModel(), KoinComponent {

    private var _tasksLoaded = false
    private var _tasksBeingListened = false

    private var _newTasksBeingListened = false

    // Single collection to track all tasks
   // private val _tasks = MutableStateFlow<List<TaskDtoWrapper>>(emptyList())
   // val tasks: StateFlow<List<TaskDtoWrapper>> get() = _tasks

    private val _tasks = MutableStateFlow<List<MutableStateFlow<TaskDto>>>(emptyList())
    val tasks: StateFlow<List<StateFlow<TaskDto>>> get() = _tasks



    fun loadTasks(){
        if(!_tasksLoaded){
            _tasksLoaded = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val selectedDeviceUuid = Global.selectedDevice.uuid
                    val response = RetrofitClient.taskConfigurationApiService.requestTaskConfigurations(selectedDeviceUuid)
                    val taskDtos = response.body()

                    if (response.isSuccessful && taskDtos != null) {
                        for(task in taskDtos){
                            addTask(task)
                        }

                    } else {
                        println("Error: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
        }
    }

    fun listenToTasksUpdates(){
        if(!_tasksBeingListened){
            _tasksBeingListened = true
            try{
                val topic =  "/server/task_progress_update/${Global.selectedDevice.uuid}"
                val mqttClient = MqttClient(
                    Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(8), null)
                MqttHandler(mqttClient, MqttDataProcessorTaskProgress(), topic)
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    fun listenToNewTasks(){
        if(!_newTasksBeingListened){
            _newTasksBeingListened = true
            try{
                val topic =  "/${Global.selectedDevice.uuid}/newTasks"
                val mqttClient = MqttClient(
                    Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(
                        8
                    ), null)
                MqttHandler(mqttClient, MqttDataProcessorNewTask(Global.selectedDevice.uuid), topic)
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }





    // Function to observe a specific task by ID
    /*fun getTaskFlow(taskId: Long): StateFlow<TaskDtoWrapper?> {
        val taskWrapper = _tasks.value.find { it.taskDto.uid == taskId }
        return MutableStateFlow(taskWrapper)
    }*/

    // Function to add or update a task
    /*fun addOrUpdateTask(task: TaskDto) {
        val existingTask = _tasks.value.find { it.taskDto.uid == task.uid }

        if (existingTask != null) {
            viewModelScope.launch {
                existingTask.setProgress(task.progress)
            }
        } else {
            // Add the new task if it doesn't exist
            _tasks.value += TaskDtoWrapper(task)
        }
    }*/

    // Function to remove a task
    /*fun removeTask(taskId: Long) {
        _tasks.value = _tasks.value.filter { it.taskDto.uid != taskId }
    }*/

    // Update progress for a specific task
   // fun updateTaskProgress(taskProgressUpdate: TaskProgressUpdate) {
    fun updateTaskProgress(taskStateInfoDto: TaskStateInfoDto) {
        /*val taskDtoWrapper = _tasks.value.find { it.taskDto.uid == taskProgressUpdate.taskUid }

        if(taskDtoWrapper != null){
            viewModelScope.launch {
                //taskDtoWrapper.setProgress(taskProgressUpdate.progress)
            }
        }*/

        val listOfMutableStateFlows = _tasks.value
        val taskMutableStateFlow = listOfMutableStateFlows.find { it.value.uid == taskStateInfoDto.taskUid }
        if(taskMutableStateFlow != null){
           /* val newValue = taskMutableStateFlow.value.copy()
            newValue.stateInfos.add(taskStateInfoDto)

            viewModelScope.launch {
                taskMutableStateFlow.value = newValue
                taskMutableStateFlow.emit(newValue)
            }*/

            //val newValue = taskMutableStateFlow.value.copy()
            //newValue.stateInfos.add(taskStateInfoDto) // reassign with a new list

            val newValue2 = taskMutableStateFlow.value.copy(
                stateInfos = taskMutableStateFlow.value.stateInfos.toMutableSet().apply {
                    add(taskStateInfoDto)
                }
            )

            viewModelScope.launch {
                taskMutableStateFlow.emit(newValue2) // emit the modified copy
            }
        }
    }

    fun addTask(taskDto: TaskDto){
        //addTask(taskDto)
        val listOfMutableStateFlows = _tasks.value
        val existingTask = listOfMutableStateFlows.find { it.value.uid == taskDto.uid }

        if (existingTask != null) {
            viewModelScope.launch {
                //existingTask.setProgress(task.progress)
                //existingTask.value.getLastState()
            }
        } else {
            // Add the new task if it doesn't exist
            _tasks.value += MutableStateFlow(taskDto)
        }
    }
}
