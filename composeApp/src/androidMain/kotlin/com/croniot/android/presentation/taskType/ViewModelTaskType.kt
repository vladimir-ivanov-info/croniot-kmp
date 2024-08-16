package com.croniot.android.presentation.taskType

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.croniot.android.Global
import croniot.models.dto.ParameterTaskDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import croniot.messages.MessageAddTask
import croniot.models.Result
import kotlinx.coroutines.withContext

class ViewModelTaskType : ViewModel() {

    val _parametersValues: MutableMap<ParameterTaskDto, MutableStateFlow<String>> = mutableMapOf()
    val parametersValues : MutableMap<ParameterTaskDto, MutableStateFlow<String>> get() = _parametersValues


    init{
        for(parameter in Global.selectedTaskType.parameters){
            _parametersValues.put(parameter, MutableStateFlow("*undefined*"))
        }
    }

    fun updateParameter(parameterUid: Long, newValue: String){

        _parametersValues.forEach { (parameter, flow) ->
            if (parameter.uid == parameterUid) {
                // Update the flow with new value
                viewModelScope.launch {
                    flow.emit(newValue)  // Use emit in a coroutine context
                }
                // or flow.value = newValue if you're not in a coroutine context or are ok with potentially blocking operations
            }
        }
    }


    suspend fun sendTaskConfiguration() : Result {

        return withContext(Dispatchers.IO){
            var result: Result

            val deviceUuid = Global.selectedDevice.uuid
            val taskUid = Global.selectedTaskType.uid

            val parametersValues = mutableMapOf<Long, String>()

            _parametersValues.forEach { (parameter, flow) ->
                parametersValues[parameter.uid] = flow.value
            }
            val messageAddTask = MessageAddTask(deviceUuid, taskUid.toString(), parametersValues)

            val gson = GsonBuilder().setPrettyPrinting().create()

            val message = gson.toJson(messageAddTask)

            result = Global.performPostRequestToEndpoint("/api/add_task", message)
            result
        }
    }
}