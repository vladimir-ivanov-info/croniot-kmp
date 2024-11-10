package com.croniot.android.presentation.device.taskTypes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.croniot.android.ui.UtilUi
import croniot.models.dto.ParameterTaskDto
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import kotlinx.coroutines.launch

@Composable
fun StatefulParameter(parameter: ParameterTaskDto, taskDto: TaskDto, viewModelTask: ViewModelTaskTypes){

    var latestTaskInfoState = taskDto.getLastState()
    var secondLatestTaskInfoState : TaskStateInfoDto? = null

    var aux = taskDto.stateInfos.toList().sortedByDescending { it.dateTime }
    if(aux != null && aux.size > 1){
        secondLatestTaskInfoState = aux[1]
    }

    if(secondLatestTaskInfoState != null){
        if(latestTaskInfoState.state != "on" && latestTaskInfoState.state != "off" && (secondLatestTaskInfoState.state == "on" || secondLatestTaskInfoState.state == "off")) { //TODO This is a quick fix, a patch.
            latestTaskInfoState = secondLatestTaskInfoState
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val checked = remember(latestTaskInfoState.state) {
        latestTaskInfoState.state == "on"
    }

    if(parameter.constraints.size == 2){ //It's a switch

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ){
            Text(
                modifier = Modifier
                    .align(Alignment.CenterStart),
                text = parameter.name,
                fontSize = UtilUi.TEXT_SIZE_4,
            )
           // if(latestTaskInfoState.state == "on" || latestTaskInfoState.state == "off"){
                Switch(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    checked = checked, //TODO! get this information from task's last state. Should have at least 1 stateful state!
                    onCheckedChange = {
                        val newValue = if (it) "on" else "off"

                        viewModelTask.updateParameter(parameter.uid, newValue)

                        coroutineScope.launch {
                            viewModelTask.sendTask()
                        }
                    }
                )
           // }
        }
    }
}