package com.croniot.client.features.tasktypes.presentation.create_task

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.croniot.client.features.tasktypes.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.core.models.TaskType
import com.croniot.client.features.tasktypes.presentation.create_task.parameter.StatefulParameter
import com.croniot.client.presentation.CroniotSlider
import com.croniot.client.presentation.components.TimePicker
import croniot.models.ParameterTypes
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    deviceUuid: String,
    taskTypeUid: Long,
    onNavigateBack: () -> Unit,
    createTaskViewModel: CreateTaskViewModel = koinViewModel(),
) {
    LaunchedEffect(deviceUuid, taskTypeUid) {
        createTaskViewModel.initialize(
            _deviceUuid = deviceUuid,
            _taskTypeUid = taskTypeUid,
        )
    }

    val taskType by createTaskViewModel.taskType.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    BackHandler { onNavigateBack() }

    LaunchedEffect(Unit) {
        createTaskViewModel.events.collect { event ->
            when (event) {
                is CreateTaskUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message.asString(context),
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = { onNavigateBack() },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }

                        Box(contentAlignment = Alignment.CenterStart) {
                            if (taskType != null) {
                                Text(text = taskType!!.name)
                            } else {
                                Text(text = "Task type")
                            }
                        }
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding ->
            if (taskType != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    TaskConfiguration(
                        deviceUuid = deviceUuid,
                        taskType = taskType!!,
                        createTaskViewModel = createTaskViewModel,
                    )
                }
            }
        },
    )
}

@Composable
private fun TaskConfiguration(
    deviceUuid: String,
    taskType: TaskType,
    createTaskViewModel: CreateTaskViewModel,
) {
    val parameters = taskType.parameters.toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.configuration),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(parameters) { currentParameter ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    when (currentParameter.type) {
                        ParameterTypes.NUMBER ->
                            CroniotSlider(
                                currentParameter,
                                onNewValue = { newValue ->
                                    createTaskViewModel.updateParameter(currentParameter.uid, newValue)
                                },
                            )
                        ParameterTypes.TIME ->
                            TimePicker(
                                currentParameter,
                                onNewValue = { newValue ->
                                    createTaskViewModel.updateParameter(currentParameter.uid, newValue)
                                },
                            )
                        ParameterTypes.STATEFUL -> {
                            val latestTaskStateInfoFlow = remember {
                                createTaskViewModel.observeTaskTypeLatestState(deviceUuid, taskType)
                            }
                            StatefulParameter(
                                deviceUuid = deviceUuid,
                                taskType = taskType,
                                parameter = currentParameter,
                                latestTaskStateInfoFlow = latestTaskStateInfoFlow,
                                onStateChanged = { newState ->
                                    createTaskViewModel.sendStatefulTask(
                                        deviceUuid = deviceUuid,
                                        taskTypeUid = taskType.uid,
                                        currentParameter.uid,
                                        newState,
                                    )
                                },
                            )
                        }
                        // TODO
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }

        // TODO instead of checking first element, check if all elements are stateful or if task is immediate
        if (parameters.size == 1 && parameters[0].type == ParameterTypes.STATEFUL) {
            // Stateful parameters don't need the Add task button to be clicked, they run on click
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { createTaskViewModel.sendTask() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(8.dp),
                ) {
                    Text(text = stringResource(R.string.add_task))
                }
            }
        }
    }
}
