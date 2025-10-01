package com.croniot.client.presentation.components.tasktypes

//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxHeight
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Slider
//import androidx.compose.material3.SliderDefaults
//import androidx.compose.material3.Switch
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableFloatStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.unit.dp
//import com.croniot.client.core.models.ParameterTask
//import com.croniot.client.core.models.TaskStateInfo
//import com.croniot.client.presentation.constants.UtilUi
//import com.croniot.client.core.models.TaskType
//import com.croniot.client.core.models.isRepresentsSlider
//import com.croniot.client.core.models.isRepresentsSwitch
//import com.croniot.client.features.tasktypes.create_task.CreateTaskViewModel
//import org.koin.compose.viewmodel.koinViewModel
//
//@Composable
//fun StatefulParameter(
//    deviceUuid: String,
//    taskType: TaskType,
//    parameter: ParameterTask,
//    createTaskViewModel: CreateTaskViewModel,// = koinViewModel(), //TODO move to parent and pass child's flow
//    statefulParameterViewModel: StatefulParameterViewModel = koinViewModel(),
//    onStateChanged : (newState: String) -> Unit
//) {
//
//    val isSlider = remember { parameter.isRepresentsSlider() }
//    val isSwitch = remember { parameter.isRepresentsSwitch() }
//
//    val latestStateFlow = remember(deviceUuid, taskType.uid) {
//        createTaskViewModel.observeTaskTypeLatestState(deviceUuid, taskType)
//    }
//
//    //val latestState: TaskStateInfo? by latestStateFlow.collectAsStateWithLifecycle()
//    val latestState: TaskStateInfo? by latestStateFlow.collectAsState()
//
//    /* val latestStateAux = latestState
//     if(latestStateAux != null && (latestStateAux.state == "CREATED" || latestStateAux.state != "RECEIVED")){
//         return
//     }*/
//
//    LaunchedEffect(Unit) {
//        statefulParameterViewModel.initialize(deviceUuid, taskType.uid, 1123123) //TOOD
//    }
//
//    val taskStateInfoSynced = statefulParameterViewModel.statefulTaskInfoParameterSynced.collectAsState()
//
//    if(isSwitch){
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(48.dp)
//                .clip(RoundedCornerShape(12.dp))
//                .background(MaterialTheme.colorScheme.primaryContainer)
//            ,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            SyncDot(
//                isSynced = taskStateInfoSynced.value,
//                modifier = Modifier.padding(horizontal = 8.dp)
//            )
//
//            Text(
//                // modifier = Modifier.align(Alignment.CenterStart),
//                modifier = Modifier.weight(1f),
//                text = parameter.name,
//                fontSize = UtilUi.TEXT_SIZE_3
//            )
//
//            if(latestState != null){
//                val checked = latestState!!.state == "on" //TODO OOOOOOOOOO
//                Switch(
//                    //modifier = Modifier.align(Alignment.CenterEnd),
//                    //enabled = taskStateInfoSynced.value,
//                    modifier = Modifier.padding(horizontal = 8.dp),
//                    checked = checked,
//                    onCheckedChange = { newChecked ->
//                        // if(taskStateInfoSynced.value){
//                        val newValue = if (newChecked) parameter.constraints["state_1"] else parameter.constraints["state_2"]
//                        if (newValue != null) {
//                            onStateChanged(newValue)
//                        }
//                        //}
//                    }
//                )
//            }
//
//
//        }
//    } else if(isSlider){
//
//        Column(modifier = Modifier
//            //.fillMaxWidth()
//            //.height(48.dp)
//            .clip(RoundedCornerShape(12.dp))
//            .background(MaterialTheme.colorScheme.primaryContainer)
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(48.dp)
//                    .clip(RoundedCornerShape(12.dp))
//                    .background(MaterialTheme.colorScheme.primaryContainer)
//                ,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//                SyncDot(
//                    isSynced = taskStateInfoSynced.value,
//                    modifier = Modifier.padding(horizontal = 8.dp)
//                )
//
//                Text(
//                    // modifier = Modifier.align(Alignment.CenterStart),
//                    modifier = Modifier.weight(1f),
//                    text = parameter.name,
//                    fontSize = UtilUi.TEXT_SIZE_3
//                )
//            }
//
//            //Text("Slider here...")
//            // val latestStateValue = latestState?.state?.toFloat() //TODO "CREATED"
//            var latestStateValue = latestState?.state
//
//
//
//
//
////            var latestStateValueFloat = latestStateValue?.toFloat() ?: 0f //TODO
//
//
//
//            //println("$latestStateValue")
//            var valor = remember { mutableFloatStateOf(0f) }
//
//            if(latestStateValue != null && latestStateValue != "CREATED" && latestStateValue != "RECEIVED") {
//                if(latestStateValue == "off" || latestStateValue == "on"){
//                    println("qweqweqweqwe")
//                }
//                valor.value = latestStateValue.toFloat()
//                // latestStateValueFloat = latestStateValue.toFloat()
//            }
//
//
//            val stepSize = parameter.constraints["stepSize"]?.toDouble()?.toInt() ?: 1
//            val steps = 100 //TODO
//            Slider(
//                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
//                // value = latestStateValue ?: 0f,
//                //value = latestStateValueFloat,
//                value = valor.value,
//                colors = SliderDefaults.colors(
//                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceBright,
//                ),
//                onValueChange = { newValue ->
//                    // Redondear al múltiplo de 10 más cercano
//                    //val stepped = (newValue / 10).toInt() * 10
//                    //onValueChange(stepped)
//                    println("abcc")
//                    valor.value = newValue
//
//                    onStateChanged(newValue.toInt().toString()) //TODO do it with a timer, only send latest value after 0.5 seconds
//                },
//                valueRange = 0f..100f,
//                // steps = 9 // 0..100 en saltos de 10 (10 pasos intermedios)
//                steps = steps // 0..100 en saltos de 10 (10 pasos intermedios)
//            )
//        }
//    } else {
//        Text(
//            text = latestState?.state?: "-",
//            fontSize = UtilUi.TEXT_SIZE_3
//        )
//    }
//
//
//}
//
//@Composable
//fun SyncDot(isSynced: Boolean, modifier: Modifier = Modifier) {
//    val color = if (isSynced) {
//        MaterialTheme.colorScheme.primary
//    } else {
//        MaterialTheme.colorScheme.error // o gris, depende de lo que quieras
//    }
//
//    Box(
//        modifier = modifier.size(16.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Box(
//            modifier = Modifier
//                .size(12.dp)
//                .background(
//                    color = color,
//                    shape = CircleShape
//                )
//        )
//    }
//}