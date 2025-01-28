package com.croniot.android.features.device.features.tasktypes

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import com.croniot.android.app.Global
import com.croniot.android.core.presentation.util.UtilUi
import com.croniot.android.features.device.features.tasks.ViewModelTasks
import croniot.models.dto.ParameterTaskDto
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatefulParameter(parameter: ParameterTaskDto,
                      viewModelTask: ViewModelTaskTypes,
                      viewModelTasks : ViewModelTasks = koinViewModel<ViewModelTasks>(
                          //viewModelStoreOwner = LocalContext.current as ComponentActivity
                                  viewModelStoreOwner = LocalActivity.current as? ViewModelStoreOwner
                                  ?: throw IllegalStateException("LocalActivity is not a ViewModelStoreOwner")
                      )
                    ){

    val tasks by viewModelTasks.tasks.collectAsState()
    val taskDtoFlow = remember(tasks) {
        tasks
            .filter { it.value.taskTypeUid == Global.selectedTaskType!!.uid }
            .maxByOrNull { it.value.getLastState().dateTime }
    }?.collectAsState()

    val taskDto = taskDtoFlow?.value

    //if(taskDto != null){
    taskDto?.let {
        var latestTaskInfoState = taskDto.getLastState()

        if(latestTaskInfoState.state != "on" && latestTaskInfoState.state != "off"){
            val secondLatestInfoState = getSecondLastState(latestTaskInfoState, taskDto)
            secondLatestInfoState?.let{
                latestTaskInfoState = secondLatestInfoState
            }
        }

        if(parameter.constraints.size == 2){ //It's a switch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ){
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterStart),
                    text = parameter.name,
                    fontSize = UtilUi.TEXT_SIZE_3,
                )
                if(latestTaskInfoState.state == "on" || latestTaskInfoState.state == "off"){
                    val checked = latestTaskInfoState.state == "on"
                    Switch(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        checked = checked, //TODO! get this information from task's last state. Should have at least 1 stateful state!
                        onCheckedChange = {
                            val newValue = if (it) parameter.constraints["state_1"] else parameter.constraints["state_2"]
                            viewModelTask.sendStatefulTask(parameter.uid, newValue!!)
                        }
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

//TODO fix this patch by adding "number" property to TaskStateInfo, so "CREATED" is 1, IoT can assign 2 to "RECEVIED" and 3 to "ON" or "OFF"
fun getSecondLastState(latestTaskInfoState: TaskStateInfoDto, taskDto: TaskDto) : TaskStateInfoDto? {
    var secondLatestTaskInfoState : TaskStateInfoDto? = null

    var aux = taskDto.stateInfos.toList().sortedByDescending { it.dateTime }
    if(aux != null && aux.size > 1){
        secondLatestTaskInfoState = aux[1]
    }

    if(secondLatestTaskInfoState != null){
        if(latestTaskInfoState.state != "on" && latestTaskInfoState.state != "off"
            && (secondLatestTaskInfoState.state == "on" || secondLatestTaskInfoState.state == "off")) { //TODO This is a quick fix, a patch.
            return secondLatestTaskInfoState
        }
    }
    return null
}