package com.croniot.android.features.device.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.croniot.android.app.Global
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.features.device.features.sensors.presentation.SensorsScreen
import com.croniot.android.features.device.features.sensors.presentation.ViewModelSensors
import com.croniot.android.features.device.features.tasks.TasksScreen
import com.croniot.android.features.device.features.tasks.ViewModelTasks
import com.croniot.android.features.device.features.tasktypes.TaskTypesScreen
import com.croniot.android.features.login.controller.LoginController
import org.koin.androidx.compose.koinViewModel

val deviceScreenTabsNames = listOf("Sensors", "Task types", "Tasks")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    navController: NavController,
    viewModelSensors: ViewModelSensors,
) {
    BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate(UiConstants.ROUTE_DEVICES)
        }
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
                                val result = navController.popBackStack()
                                if (!result) {
                                    navController.navigate(UiConstants.ROUTE_DEVICES)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }

                        Box(contentAlignment = Alignment.CenterStart) {
                            val selectedDevice = Global.selectedDevice // TODO save persistently as accounUuid/deviceUuid
                            selectedDevice?.let {
                                Text(text = selectedDevice.name)
                            } ?: LoginController.forceLogOut(navController)
                        }
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = {
                innerPadding ->
            DeviceScreenContent(navController, innerPadding = innerPadding, viewModelSensors)
        },
    )
}

@Composable
fun DeviceScreenContent(navController: NavController, innerPadding: PaddingValues, viewModelSensors: ViewModelSensors) {
    val viewModelDeviceScreen: DeviceScreenViewModel = koinViewModel<DeviceScreenViewModel>(
        // viewModelStoreOwner = LocalContext.current as ComponentActivity
        // viewModelStoreOwner = LocalActivity.current
        viewModelStoreOwner = LocalActivity.current as? ViewModelStoreOwner
            ?: throw IllegalStateException("LocalActivity is not a ViewModelStoreOwner"),
    )

    val viewModelTasks: ViewModelTasks = koinViewModel<ViewModelTasks>(
        // viewModelStoreOwner = LocalContext.current as ComponentActivity
        viewModelStoreOwner = LocalActivity.current as? ViewModelStoreOwner
            ?: throw IllegalStateException("LocalActivity is not a ViewModelStoreOwner"),
    )

    LaunchedEffect(Unit) {
        viewModelTasks.loadTasks()
        viewModelTasks.listenToTasksUpdates()
        viewModelTasks.listenToNewTasks()
    }

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
            0 -> SensorsScreen(navController, viewModelSensors)
            1 -> TaskTypesScreen(navController)
            2 -> TasksScreen(navController, viewModelTasks)
        }
    }
}
