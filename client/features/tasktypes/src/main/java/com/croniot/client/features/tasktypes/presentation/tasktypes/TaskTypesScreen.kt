package com.croniot.client.features.tasktypes.presentation.tasktypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.croniot.client.core.models.Device
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TaskTypesScreen(
    selectedDevice: Device, // TODO: idealmente pasar solo deviceUuid y obtener por VM
    taskTypesViewModel: TaskTypesViewModel = koinViewModel(),
    onTaskTypeClicked: (deviceUuid: String, taskTypeUid: Long) -> Unit,
) {
    LaunchedEffect(Unit) {
        taskTypesViewModel.initialize(selectedDevice.uuid, selectedDevice.taskTypes)
    }

    val tasksTypes = selectedDevice.taskTypes

    Box(modifier = Modifier.fillMaxSize()) {
        if (tasksTypes.isEmpty()) {
            Text(
                text = "No task types available",
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = tasksTypes,
                key = { taskType -> "${selectedDevice.uuid}|taskType:${taskType.uid}" }, // clave única y estable
            ) { task ->
                TaskTypeItem(
                    taskType = task,
                    secondaryTextFlow = taskTypesViewModel.getSecondaryText(selectedDevice.uuid, task),
                    onTaskTypeClicked = {
                        onTaskTypeClicked(selectedDevice.uuid, task.uid)
                    }
                )
            }
        }
    }
}
