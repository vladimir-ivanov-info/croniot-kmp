package com.croniot.android.features.device.features.tasks

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.croniot.android.R
// import com.croniot.android.app.Global
// import com.croniot.android.core.presentation.util.UtilUi
import com.croniot.client.core.util.DateTimeUtil
import com.croniot.client.presentation.constants.UtilUi
import croniot.models.TaskState
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel

import com.croniot.client.core.models.Task

@Composable
fun TasksScreen(
    selectedDeviceUuid: String,
    navController: NavController,
    tasksViewModel: TasksViewModel = koinViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> tasksViewModel.initialize(selectedDeviceUuid)
                Lifecycle.Event.ON_STOP -> tasksViewModel.stopObserving()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val tasks by tasksViewModel.tasks.collectAsStateWithLifecycle()
    val sortedTasks = remember(tasks) {
        tasks.sortedByDescending { it.value.initialTaskStateInfo?.dateTime }
    }

    if (tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(
                items = sortedTasks,
                key = { _, taskFlow -> "${taskFlow.value.deviceUuid}|${taskFlow.value.uid}" },
            ) { _, taskFlow ->
                val task = taskFlow.value
                val taskTypeName = remember(task.deviceUuid, task.taskTypeUid) {
                    tasksViewModel.getTaskType(task.deviceUuid, task.taskTypeUid)?.name
                }
                GenericTaskItem(
                    taskStateFlow = taskFlow,
                    taskTypeName = taskTypeName ?: "[task_name]",
                    onTaskClicked = { taskToShow ->
                        // Handle click events for tasks
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun GenericTaskItem(
    taskStateFlow: StateFlow<Task>,
    taskTypeName: String,
    onTaskClicked: (taskClicked: Task) -> Unit,
) {
    val taskValue by taskStateFlow.collectAsStateWithLifecycle()

    val taskName = taskTypeName

    val stateIconPainter: Painter
    var stateIconColor = MaterialTheme.colorScheme.onSurface

    val latestStateInfo = taskValue.initialTaskStateInfo
    var latestStateInfoProgress = latestStateInfo?.progress ?: 0.0

    when (latestStateInfo?.state) {
        TaskState.CREATED.name -> {
            stateIconPainter = painterResource(id = R.drawable.baseline_schedule_24)
        }
        TaskState.RUNNING.name -> {
            stateIconPainter = painterResource(id = R.drawable.baseline_update_24)
        }
        TaskState.COMPLETED.name -> {
            stateIconPainter = painterResource(id = R.drawable.baseline_done_24)
            stateIconColor = MaterialTheme.colorScheme.primary
        }
        "on" -> {
            stateIconPainter = painterResource(id = R.drawable.ic_toggle_on_24)
            stateIconColor = MaterialTheme.colorScheme.primary
        }
        "off" -> {
            stateIconPainter = painterResource(id = R.drawable.ic_toggle_off_24)
            stateIconColor = MaterialTheme.colorScheme.error
        }
        "RECEIVED" -> {
            stateIconPainter = painterResource(id = R.drawable.ic_check_24)
            stateIconColor = MaterialTheme.colorScheme.tertiary
        }
        else -> {
            stateIconPainter = painterResource(id = R.drawable.baseline_question_mark_24)
        }
    }
    // }

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
                .fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(),
        ) {
// TODO make this a method in another class

            // val selectedDevice = localDataRepository.getSelectedDevice()//.collectAsState(null)

            // Retrieve task name
//            val taskName = remember(taskValue.taskTypeUid) {
//              //  Global.selectedDevice?.taskTypes?.firstOrNull { it.uid == taskValue.taskTypeUid }?.name.orEmpty() // TODO
//                /*selectedDevice.value?.let { device ->
//                    device.taskTypes.firstOrNull{ it.uid == taskValue.taskTypeUid }?.name.orEmpty()
//                }*/
//
//                selectedDevice.taskTypes.
//                firstOrNull{ it.uid == taskValue.taskTypeUid }?.name.orEmpty()
//            }
// TODO_end
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,

                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = stateIconPainter,
                            contentDescription = null,
                            tint = stateIconColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Text(
                        text = taskName ?: "[task_name]",
                        // text = "[task_name]",
                        // text = viewModelTasks.getTaskType(),
                        fontSize = UtilUi.TEXT_SIZE_3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var latestStateInfoProgressText = "$latestStateInfoProgress %"

                    if (latestStateInfo?.state != TaskState.RUNNING.name) {
                        latestStateInfoProgressText = ""
                    }

                    Text(
                        text = latestStateInfoProgressText,
                        fontSize = 14.sp,

                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (latestStateInfo != null) {
                        val formattedDateTimeAgain = remember(latestStateInfo.dateTime) {
                            DateTimeUtil.formatRelativeTime(latestStateInfo.dateTime)
                        }
                        Text(
                            text = formattedDateTimeAgain,
                            fontSize = 14.sp,

                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
