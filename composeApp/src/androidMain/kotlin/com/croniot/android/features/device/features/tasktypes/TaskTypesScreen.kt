package com.croniot.android.features.device.features.tasktypes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.croniot.android.app.Global
import croniot.models.dto.TaskTypeDto

@Composable
fun TaskTypesScreen(navController: NavController) {
    val selectedDevice = Global.selectedDevice

    var tasks = emptyList<TaskTypeDto>()

    // if(selectedDevice != null){
    selectedDevice?.let {
        tasks = selectedDevice.tasks.toList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
        ) {
            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }
            items(tasks) { item ->
                TaskItem(navController, task = item)
                Spacer(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
