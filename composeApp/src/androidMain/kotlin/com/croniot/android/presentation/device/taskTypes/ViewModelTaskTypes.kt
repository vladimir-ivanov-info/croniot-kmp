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
        val selectedTaskType = Global.selectedTaskType
        //if(selectedTaskType != null){
        selectedTaskType?.let{
            for(parameter in selectedTaskType.parameters){

                with(parameter){
                    if(type == "number"){
                        val minValue = constraints["minValue"]
                        val maxValue = constraints["maxValue"]
                        val midValue = ((maxValue!!.toDouble() - minValue!!.toDouble())/2).toString()
                        _parametersValues.put(parameter, MutableStateFlow(midValue))
                    } else {
                        _parametersValues.put(parameter, MutableStateFlow(PARAMETER_VALUE_UNDEFINED))
                    }
                }
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

    fun sendStatefulTask(parameterUid: Long, newValue: String){
        viewModelScope.launch(Dispatchers.IO){
            val selectedDevice = Global.selectedDevice

            if(selectedDevice != null){
                var result: Result

                val deviceUuid = selectedDevice.uuid
                val taskUid = Global.selectedTaskType!!.uid

                val parametersValues = mutableMapOf<Long, String>()

                parametersValues[parameterUid] = newValue

                val messageAddTask = MessageAddTask(deviceUuid, taskUid.toString(), parametersValues)

                val gson = GsonBuilder().setPrettyPrinting().create()
                val message = gson.toJson(messageAddTask)
                result = Global.performPostRequestToEndpoint("/api/add_task", message) //50 ms //TODO do something if result is false
            } else {
                Result(false, "Selected Device is null")
            }
        }
    }

    suspend fun sendTask() : Result {

        return withContext(Dispatchers.IO){

            val selectedDevice = Global.selectedDevice

            if(selectedDevice != null){
                var result: Result

                val deviceUuid = selectedDevice.uuid
                val taskUid = Global.selectedTaskType!!.uid

                val parametersValues = mutableMapOf<Long, String>()

                _parametersValues.forEach { (parameter, flow) ->
                    parametersValues[parameter.uid] = flow.value
                }
                val messageAddTask = MessageAddTask(deviceUuid, taskUid.toString(), parametersValues)

                val gson = GsonBuilder().setPrettyPrinting().create()

                val message = gson.toJson(messageAddTask)

                result = Global.performPostRequestToEndpoint("/api/add_task", message)
                result
            } else {
                Result(false, "Selected Device is null")
            }
        }
    }
}