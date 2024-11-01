package com.croniot.android.presentation.device.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.croniot.android.Global
import com.croniot.android.R
import com.croniot.android.domain.util.DateTimeUtil
import com.croniot.android.ui.UtilUi
import com.croniot.android.ui.task.ViewModelTasks
import com.croniot.android.ui.util.FixedSizeScrollableDialog
import croniot.models.TaskState
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel

@Composable
fun TasksScreen(navController: NavController){

    val viewModelTasks: ViewModelTasks = koinViewModel()

    val tasks by viewModelTasks.tasks.collectAsState()

    val tasksSortedByDate = tasks.toList().sortedByDescending { it.value.getLastState().dateTime }

    var showTask by remember{ mutableStateOf(false) }
    var taskToShow by remember{ mutableStateOf(TaskDto()) }

    Box(modifier = Modifier
        .fillMaxSize()){

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }

            items(tasksSortedByDate){ item ->
                //TODO generic task card. For specific cards, create custom for your custom task type!
                GenericTaskItem(
                    navController, taskStateFlow = item, onTaskClicked = {
                    taskToShow = it
                    showTask = true
                })
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        if(showTask){
            FixedSizeScrollableDialog(taskToShow, onDismissRequest = { showTask = false })
        }
    }
}

@Composable
fun GenericTaskItem(navController: NavController, taskStateFlow: StateFlow<TaskDto>, onTaskClicked: (taskClicked: TaskDto) -> Unit) {

    val taskAsState = taskStateFlow.collectAsState()
    val taskValue = taskAsState.value

    var stateIconPainter = painterResource(id = R.drawable.baseline_done_24)
    var stateIconColor = Color.Black

    val stateInfos = taskValue.stateInfos.toList().sortedByDescending { it.dateTime }
    var latestStateInfo : TaskStateInfoDto? = null
    var latestStateInfoProgress = 0.0

    if(stateInfos.isNotEmpty()){
        latestStateInfo = stateInfos[0]
        latestStateInfoProgress = latestStateInfo.progress

        if(latestStateInfo.state == TaskState.CREATED){
            stateIconPainter = painterResource(id = R.drawable.baseline_schedule_24)
        } else if(latestStateInfo.state == TaskState.RUNNING){
            stateIconPainter = painterResource(id = R.drawable.baseline_update_24)
            //stateIconColor = Color.Blue
        } else if(latestStateInfo.state == TaskState.COMPLETED){
            stateIconPainter = painterResource(id = R.drawable.baseline_done_24)
            stateIconColor = Color.Green
        } else {
            stateIconPainter = painterResource(id = R.drawable.baseline_question_mark_24)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable {
                onTaskClicked(taskValue)
            },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
            ,
            elevation = CardDefaults.elevatedCardElevation()
        ) {
//TODO make this a method in another class
            var taskName = ""

            val selectedDevice = Global.selectedDevice
            for (taskType in selectedDevice.tasks){
                if(taskType.uid == taskValue.taskUid){
                    taskName = taskType.name
                }
            }
//TODO_end
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ){
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ){
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White, shape = CircleShape)
                                    ,
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = stateIconPainter,
                                    contentDescription = null,
                                    tint = stateIconColor,
                                    modifier = Modifier.size(24.dp)
                                    ,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                            ,
                            contentAlignment = Alignment.CenterStart
                        ){
                            Text(
                                text = taskName,
                                fontSize = UtilUi.TEXT_SIZE_3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp)
                                ,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "$latestStateInfoProgress %",
                            fontSize = 14.sp,

                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if(latestStateInfo != null){
                        val formattedDateTimeAgain = DateTimeUtil.formatRelativeTime(latestStateInfo.dateTime)
                        Box(
                            //modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer)
                            //,
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = formattedDateTimeAgain,
                                fontSize = 14.sp,

                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}