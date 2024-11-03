package com.croniot.android.presentation.device.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.croniot.android.Global
import com.croniot.android.R
import com.croniot.android.domain.util.DateTimeUtil
import com.croniot.android.ui.UtilUi
import com.croniot.android.ui.task.ViewModelTasks
import croniot.models.TaskState
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TasksScreen(
    navController: NavController,
    viewModelTasks: ViewModelTasks
) {
    // Collect tasks and sort them only once
    val tasks by viewModelTasks.tasks.collectAsState()
    val sortedTasks = remember(tasks) { tasks.toList().sortedByDescending { it.value.getLastState().dateTime } }

    // Batch size management
    var batchSize by remember { mutableStateOf(10) } // Initial batch size
    val scrollState = rememberScrollState()

    // Increase batch size as the user scrolls closer to the end
    LaunchedEffect(scrollState.value) {
        val scrollRange = scrollState.maxValue - scrollState.value
        if (scrollRange < 200 && batchSize < sortedTasks.size) {
            batchSize += 10
        }
    }

    // Display a loading indicator if tasks are initially empty
    if (tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Display only the current batch
            itemsIndexed(sortedTasks.take(batchSize)) { index, taskFlow ->
                GenericTaskItem(
                    navController = navController,
                    taskStateFlow = taskFlow,
                    onTaskClicked = { taskToShow ->
                        // Handle click events for tasks
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun GenericTaskItem(navController: NavController, taskStateFlow: StateFlow<TaskDto>, onTaskClicked: (taskClicked: TaskDto) -> Unit) {

    val taskValue by taskStateFlow.collectAsState()

    val stateIconPainter: Painter
    var stateIconColor = Color.Black

    val stateInfos = taskValue.stateInfos.toList().sortedByDescending { it.dateTime }
    var latestStateInfo : TaskStateInfoDto? = null
    var latestStateInfoProgress = 0.0

   // if(stateInfos.isNotEmpty()){ //TODO not necessary, every task has at least 1 state
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
    //}

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
           /* var taskName = ""

            val selectedDevice = Global.selectedDevice
            for (taskType in selectedDevice.tasks){
                if(taskType.uid == taskValue.taskUid){
                    taskName = taskType.name
                }
            }*/

            // Retrieve task name
            val taskName = remember(taskValue.taskUid) {
                Global.selectedDevice.tasks.firstOrNull { it.uid == taskValue.taskUid }?.name.orEmpty()
            }
//TODO_end
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ){
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                    .weight(1f),
                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .fillMaxHeight()
                            .background(Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = stateIconPainter,
                            contentDescription = null,
                            tint = stateIconColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Text(
                        text = taskName,
                        fontSize = UtilUi.TEXT_SIZE_3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp)
                        ,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$latestStateInfoProgress %",
                        fontSize = 14.sp,

                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if(latestStateInfo != null){
                        val formattedDateTimeAgain = remember(latestStateInfo.dateTime) {
                            DateTimeUtil.formatRelativeTime(latestStateInfo.dateTime)
                        }
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