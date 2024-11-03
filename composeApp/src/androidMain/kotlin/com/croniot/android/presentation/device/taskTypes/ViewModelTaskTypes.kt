package com.croniot.android.presentation.device.taskTypes

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

class ViewModelTaskTypes : ViewModel() {

    companion object{
        const val PARAMETER_VALUE_UNDEFINED : String = "*undefined*"
    }

    private val _parametersValues: MutableMap<ParameterTaskDto, MutableStateFlow<String>> = mutableMapOf()
    val parametersValues : MutableMap<ParameterTaskDto, MutableStateFlow<String>> get() = _parametersValues


    //TODO delegate this to another class, there will be more parameter types in the future
    init{
        for(parameter in Global.selectedTaskType.parameters){

            if(parameter.type == "number"){
                val minValue = parameter.constraints["minValue"]
                val maxValue = parameter.constraints["maxValue"]
                val midValue = ((maxValue!!.toDouble() - minValue!!.toDouble())/2).toString()
                _parametersValues.put(parameter, MutableStateFlow(midValue))
            } else {
                _parametersValues.put(parameter, MutableStateFlow(PARAMETER_VALUE_UNDEFINED))
            }
        }
    }

    fun uninit(){
        _parametersValues.values.clear()
    }

    fun updateParameter(parameterUid: Long, newValue: String){

        _parametersValues.forEach { (parameter, flow) ->
            if (parameter.uid == parameterUid) {
                viewModelScope.launch {
                    flow.emit(newValue)  // Use emit in a coroutine context
                }
            }
        }
    }

    suspend fun sendTask() : Result {

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