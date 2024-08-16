package com.croniot.android.presentation.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.croniot.android.ui.PerformanceChart
import com.croniot.android.ui.UtilUi
import kotlinx.coroutines.flow.MutableStateFlow
import croniot.models.SensorData
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.croniot.android.Global
import com.croniot.android.ViewModelSensorData
import com.croniot.android.ui.TaskItem
import com.croniot.android.presentation.task.ScreenTask
import com.croniot.android.ui.task.ViewModelTask
import croniot.models.dto.SensorDto
import org.koin.java.KoinJavaComponent.get

private val viewModelSensorData = get<ViewModelSensorData>(ViewModelSensorData::class.java)
private val viewModelDeviceScreen = get<DeviceScreenViewModel>(DeviceScreenViewModel::class.java)

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
                            Text(text = com.croniot.android.Global.selectedDevice.name)
                        }

                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = {
            innerPadding -> DeviceScreenContent(navController, innerPadding = innerPadding)
        }
    )
}

@Composable
fun DeviceScreenContent(navController: NavController, innerPadding: PaddingValues){

    val currentTab = viewModelDeviceScreen.currentTab.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(currentTab.value) }
    val tabs = listOf("Sensors", "Task types", "Tasks")

    val viewModelTask: ViewModelTask = viewModel()

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
            0 -> SensorsScreen(viewModelSensorData)
            1 -> TaskTypesScreen(navController)
            2 -> ScreenTask(navController, viewModelTask)
        }
    }
}
@Composable
fun SensorsScreen(viewModelSensorData: ViewModelSensorData){
    val sensorDataMap = viewModelSensorData.map

    //TODO observe not map, but map values
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

            items(sensorDataMap.toList()) { item ->
                val (key, sensorDataFlow) = item
                SensorItem(key, sensorDataFlow)
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }
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


@Composable
fun SensorItem(sensor: SensorDto, sensorDataFlow: MutableStateFlow<SensorData>) {

    val sensorDataState = sensorDataFlow.collectAsState()
    val sensorData = sensorDataState.value

    val sensorName = sensor.parameters.first().name //TODO adapt for when there are many parameters in a sensor
    val sensorUnit = sensor.parameters.first().unit //TODO adapt for when there are many parameters in a sensor
    val sensorDataValue = sensorData.value

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        elevation = CardDefaults.elevatedCardElevation()

    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                //.background(Colors.primary),
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = sensorDataValue + " " + sensorUnit,

                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.Center),
            )

            Text(
                text = sensorName,
                fontSize = UtilUi.TEXT_SIZE_3,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.BottomCenter),
            )

            val list = remember { mutableListOf<Float>() }

            if(!sensorData.value.equals("empty_value")){
                try{
                    val data = sensorData.value.toFloat()
                    list.add(data)

                    if(list.size > 100){
                        list.removeFirst()
                    }

                } catch (e: Throwable){
                    println()
                }
            }
            PerformanceChart(sensor, modifier = Modifier, list)
        }
    }
}