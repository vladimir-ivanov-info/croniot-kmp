package com.croniot.android.features.device.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.croniot.android.features.device.features.sensors.presentation.SensorsScreen
import com.croniot.android.features.device.features.tasks.TasksScreen
import com.croniot.client.core.models.Device
import com.croniot.client.features.tasktypes.presentation.tasktypes.TaskTypesScreen
import com.croniot.client.presentation.constants.UiConstants
import org.koin.androidx.compose.koinViewModel

// val deviceScreenTabsNames = listOf("Sensors", "Task types", "Tasks")
val deviceScreenTabsNames = listOf("Sensors", "Task types")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    selectedDeviceUuid: String,
    navController: NavController,
    viewModelDeviceScreen: DeviceScreenViewModel = koinViewModel(),
    onTaskTypeClicked: (deviceUuid: String, taskTypeUid: Long) -> Unit,
) {
    val device = viewModelDeviceScreen.device.value

    LaunchedEffect(Unit) {
        viewModelDeviceScreen.initialize(selectedDeviceUuid)
    }

    BackHandler {
        if (!navController.popBackStack()) {
            // viewModel.resetCurrentScreen() //TODO
            navController.navigate(UiConstants.ROUTE_DEVICES)
        }
    }

    SideEffect {
        // viewModel.saveCurrentScreen() //TODO
        // viewModelDeviceScreen.saveCurrentScreen()
    }

    Scaffold(
        topBar = {
            TopAppBar( // This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = {
                                // val result = navController.popBackStack()
                                // if (!result) {
                                navController.navigate(UiConstants.ROUTE_DEVICES)
                                // }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back",
                            )
                        }

                        Box(contentAlignment = Alignment.CenterStart) {
                            device?.let {
                                Text(text = device.name)
                            } // TODO ?: LoginController.forceLogOut(navController)
                        }
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding ->
            if (device != null) {
                DeviceScreenContent(
                    selectedDevice = device,
                    navController = navController,
                    innerPadding = innerPadding,
                    viewModelDeviceScreen = viewModelDeviceScreen,
                    onTaskTypeClicked = { deviceUuid, taskUid ->
                        onTaskTypeClicked(deviceUuid, taskUid)
                    },
                )
            } else {
                // TODO loading...
            }
        },
    )
}

@Composable
fun DeviceScreenContent(
    selectedDevice: Device,
    navController: NavController,
    innerPadding: PaddingValues,
    onTaskTypeClicked: (deviceUuid: String, taskTypeUid: Long) -> Unit,
    viewModelDeviceScreen: DeviceScreenViewModel, // = koinViewModel()
) {
    val currentTab = viewModelDeviceScreen.currentTab.collectAsState()

    var selectedTabIndex = currentTab.value

    Column(modifier = Modifier.padding(innerPadding)) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
        ) {
            deviceScreenTabsNames.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        viewModelDeviceScreen.updateCurrentTab(index)
                        selectedTabIndex = index
                    },
                    text = { Text(title) },
                )
            }
        }
        when (selectedTabIndex) {
            0 -> SensorsScreen(
                selectedDevice,
                navController,
            )
            1 -> TaskTypesScreen(
                selectedDevice = selectedDevice,
                onTaskTypeClicked = { deviceUuid, taskTypeUid ->
                    onTaskTypeClicked(deviceUuid, taskTypeUid)
                },
            )
            2 -> TasksScreen(
                selectedDeviceUuid = selectedDevice.uuid,
                navController = navController,
            )
        }
    }
}
