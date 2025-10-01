package com.croniot.client.features.tasktypes.presentation.create_task

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.croniot.client.core.models.TaskType
import com.croniot.client.features.tasktypes.presentation.create_task.parameter.StatefulParameter
import com.croniot.client.presentation.CroniotSlider
import com.croniot.client.presentation.components.GenericDialog
import com.croniot.client.presentation.components.TimePicker
import com.croniot.client.presentation.constants.UiConstants
import croniot.models.ParameterTypes
import croniot.models.Result
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    deviceUuid: String,
    taskTypeUid: Long,
    navController: NavController,
    createTaskViewModel: CreateTaskViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        createTaskViewModel.initialize(
            _deviceUuid = deviceUuid,
            _taskTypeUid = taskTypeUid,
        )
    }

    val taskType = createTaskViewModel.taskType.value

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate(UiConstants.ROUTE_DEVICE)
        }
    }

    LaunchedEffect(Unit) {
        createTaskViewModel.events.collect { event ->
            when (event) {
                is CreateTaskUiEvent.ShowSnackbar -> {
                    val eventResult = event.result

                    var snackBarMessage = "Task sent successfully"

                    if (!eventResult.success) {
                        snackBarMessage = "Failed sending task: ${eventResult.message}"
                    }

                    val result = snackbarHostState.showSnackbar(
                        message = snackBarMessage,
                        // actionLabel = event.action
                        // actionLabel = "Dismiss",
                        duration = SnackbarDuration.Short,
                    )
                    // si necesitas actuar según el click de acción:
                    // if (result == SnackbarResult.ActionPerformed) { ... }
                }
                // UiEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
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
                            onClick = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(UiConstants.ROUTE_DEVICE)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                // Add some space between the icon and the text
                            )
                        }

                        Box(contentAlignment = Alignment.CenterStart) {
                            // Text(text = Global.selectedTaskType!!.name)
                            if (taskType != null) {
                                Text(text = taskType.name)
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
                TaskScreenContent(
                    deviceUuid,
                    taskType,
                    navController,
                    innerPadding,
                    snackbarHostState,
                    createTaskViewModel,
                )
            } else {
                // TODO show loading
            }
        },
    )
}

@Composable
fun TaskScreenContent(
    deviceUuid: String,
    taskType: TaskType,
    navController: NavController,
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    createTaskViewModel: CreateTaskViewModel,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        TaskConfiguration(
            deviceUuid,
            taskType,
            navController,
            snackbarHostState,
            createTaskViewModel,
        )
    }
}

@Composable
fun TaskConfiguration(
    deviceUuid: String,
    taskType: TaskType,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    createTaskViewModel: CreateTaskViewModel,
) {
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var postNewTaskResult by remember { mutableStateOf(Result(false, "")) }

    /*LaunchedEffect(Unit) {
        createTaskViewModel.initialize(deviceUuid, taskType)
    }*/

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
                text = "Configuration",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            // items(viewModelTaskTypes.parametersValues.size) { index ->
            items(
                taskType.parameters.size,
            ) { index ->

                Row(modifier = Modifier.fillMaxWidth()) {
                    val currentParameter = taskType.parameters.toList()[index]

                    when (currentParameter.type) {
                        ParameterTypes.NUMBER -> CroniotSlider(
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
                            val latestTaskStateInfoFlow = createTaskViewModel.observeTaskTypeLatestState(deviceUuid, taskType)

                            StatefulParameter(
                                deviceUuid = deviceUuid,
                                taskType = taskType,
                                parameter = currentParameter,
                                // createTaskViewModel = createTaskViewModel,
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

        // TODO
        /*LaunchedEffect(Unit) {
            if (Global.selectedTaskType == null || viewModelTaskTypes.parametersValues.isEmpty()) {
                if (!navController.popBackStack()) {
                    navController.navigate(UiConstants.ROUTE_DEVICE)
                }
            }
        }*/

        if (taskType.parameters.toList().size == 1 && taskType.parameters.toList()[0].type == "stateful") {
            // Stateful parameters don't need the Add task button to be clicked, they run on click
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        createTaskViewModel.sendTask()
                        // TODO add confirmation dialog
                        // TODO go back?
                        coroutineScope.launch {
                            // TODO postNewTaskResult = viewModelTaskTypes.sendTask()
                            if (postNewTaskResult.success) {
                                snackbarHostState.showSnackbar(
                                    message = "Task created successfully.",
                                    actionLabel = "Dismiss",
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(8.dp),
                ) {
                    Text(text = "Add task")
                }
            }
            if (showDialog) {
                GenericDialog(
                    title = "New task",
                    text = postNewTaskResult.message,
                    button1Text = "Accept",
                    onButton1Clicked = { showDialog = false },
                    button2Text = "Go to Tasks",
                    onButton2Clicked = {
                        // TODO not active for now viewModelDeviceScreen.updateCurrentTab(2) //TODO make enum
                    },
                ) {
                }
            }
        }
    }
}
