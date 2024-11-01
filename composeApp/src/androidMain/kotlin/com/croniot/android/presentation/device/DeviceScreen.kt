package com.croniot.android.presentation.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.croniot.android.Global
import com.croniot.android.presentation.device.sensors.SensorsScreen
import com.croniot.android.ui.TaskItem
import com.croniot.android.presentation.device.tasks.TasksScreen
import com.croniot.android.ui.task.ViewModelTasks
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(navController: NavController, modifier: Modifier) {
    Scaffold(
        topBar = {
            TopAppBar( //This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            modifier = modifier.padding(end = 8.dp),
                            onClick = {
                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }

                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(text = Global.selectedDevice.name)
                        }

                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = {
            innerPadding -> DeviceScreenContent(navController, innerPadding = innerPadding, viewModelTasks = koinViewModel())
        }
    )
}

@Composable
fun DeviceScreenContent(navController: NavController, innerPadding: PaddingValues, viewModelTasks: ViewModelTasks){
    val viewModelDeviceScreen: DeviceScreenViewModel = koinViewModel()

    LaunchedEffect(Unit) {
        viewModelTasks.loadTasks()
        viewModelTasks.listenToTasksUpdates()
        viewModelTasks.listenToNewTasks()
    }

    val currentTab = viewModelDeviceScreen.currentTab.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(currentTab.value) }
    val tabs = listOf("Sensors", "Task types", "Tasks")

    Column(modifier = Modifier.padding(innerPadding)) {
        TabRow(
            selectedTabIndex = selectedTabIndex
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        viewModelDeviceScreen.updateCurrentTab(index)
                        selectedTabIndex = index
                    },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTabIndex) {
            0 -> SensorsScreen()
            1 -> TaskTypesScreen(navController)
            2 -> TasksScreen(navController)
        }
    }
}

@Composable
fun TaskTypesScreen(navController: NavController){
    val selectedDevice = Global.selectedDevice
    val tasks = selectedDevice.tasks.toList()

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
            items(tasks){ item ->
                TaskItem(navController, task = item)
                Spacer(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

