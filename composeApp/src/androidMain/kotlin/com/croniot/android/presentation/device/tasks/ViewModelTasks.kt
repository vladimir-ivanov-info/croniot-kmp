package com.croniot.android.ui.task

import MqttHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.Global
import com.croniot.android.data.source.remote.retrofit.RetrofitClient
import com.croniot.android.presentation.device.tasks.MqttDataProcessorNewTask
import com.croniot.android.presentation.device.tasks.MqttDataProcessorTaskProgress
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.koin.core.component.KoinComponent

class ViewModelTasks : ViewModel(), KoinComponent {

    private var _tasksLoaded = false
    private var _tasksBeingListened = false

    private var _newTasksBeingListened = false

    private val _tasks = MutableStateFlow<List<MutableStateFlow<TaskDto>>>(emptyList())
    val tasks: StateFlow<List<StateFlow<TaskDto>>> get() = _tasks

    fun uninit(){
        _tasks.value = mutableStateListOf()

        _tasksLoaded = false
        _tasksBeingListened = false
        _newTasksBeingListened = false
    }

    fun loadTasks(){
        if(!_tasksLoaded){
            _tasksLoaded = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val selectedDeviceUuid = Global.selectedDevice?.uuid //TODO
                    // val response = RetrofitClient.taskConfigurationApiService.requestTaskConfigurations(selectedDeviceUuid) //TODO
                    val response = selectedDeviceUuid?.let {
                        RetrofitClient.taskConfigurationApiService.requestTaskConfigurations(
                            it
                        )
                    }
                    val taskDtos = response?.body()

                    if (response != null) {
                        if (response.isSuccessful && taskDtos != null) {
                            for(task in taskDtos){
                                addTask(task)
                            }

                        } else {
                            println("Error: ${response.errorBody()?.string()}")
                        }
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
                val topic = "/server/task_progress_update/${Global.selectedDevice?.uuid}"
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
                val topic =  "/${Global.selectedDevice?.uuid}/newTasks"
                val mqttClient = MqttClient(
                    Global.mqttBrokerUrl, Global.mqttClientId + Global.generateUniqueString(
                        8
                    ), null)

                val selectedDevice = Global.selectedDevice
                if(selectedDevice != null){
                    MqttHandler(mqttClient, MqttDataProcessorNewTask(selectedDevice.uuid), topic)
                }

            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    fun updateTaskProgress(taskStateInfoDto: TaskStateInfoDto) {

        val listOfMutableStateFlows = _tasks.value
        val taskMutableStateFlow = listOfMutableStateFlows.find { it.value.uid == taskStateInfoDto.taskUid }


        if(taskMutableStateFlow != null){
            val newValue2 = taskMutableStateFlow.value.copy(
                stateInfos = taskMutableStateFlow.value.stateInfos.toMutableSet().apply {
                    add(taskStateInfoDto)
                }
            )
            println(newValue2.taskUid)
            taskMutableStateFlow.value = newValue2 //TODO check why don't use emit here
        }
    }

    fun addTask(taskDto: TaskDto){
        val listOfMutableStateFlows = _tasks.value
        val existingTask = listOfMutableStateFlows.find { it.value.uid == taskDto.uid }

        if (existingTask != null) {
            viewModelScope.launch {

            }
        } else {
            // Add the new task if it doesn't exist
            _tasks.value += MutableStateFlow(taskDto)
        }
    }
}
