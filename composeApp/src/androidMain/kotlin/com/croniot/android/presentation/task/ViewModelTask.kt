package com.croniot.android.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.data.source.remote.retrofit.RetrofitClient
import croniot.models.dto.TaskDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class ViewModelTask : ViewModel(), KoinComponent {

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())
    val tasks: StateFlow<List<TaskDto>> get() = _tasks

    fun loadTasks(){
        viewModelScope.launch {
             try {
                val selectedDeviceUuid = com.croniot.android.Global.selectedDevice.uuid
                val response = RetrofitClient.taskConfigurationApiService.requestTaskConfigurations(selectedDeviceUuid)
                val tasksResponse = response.body()
                if (response.isSuccessful && tasksResponse != null) {
                    _tasks.emit(tasksResponse)
                } else {
                    println("Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
}