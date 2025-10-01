package com.croniot.client.features.tasktypes.presentation.tasktypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.croniot.client.core.models.Device
/*
@Composable
fun TaskTypesScreen(
    selectedDevice: Device, //TODO pass deviceUuid and fetch via viewmodel/repository
    onTaskTypeClicked: (deviceUuid: String, taskTypeUid: Long) -> Unit
) {

    val tasks = selectedDevice.taskTypes.toList()

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
            items(tasks) { task ->
                TaskTypeItem(
                    task = task,
                    onTaskTypeClicked = {

                        //TODO if it's stateful, send query to server so it asks iot to state update
                        //RequestTaskStateInfoSyncIfNecessaryUseCase(device_uuid, task_type)

                        onTaskTypeClicked(
                            selectedDevice.uuid,
                            task.uid
                        )
                    }
                )
                Spacer(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}*/

@Composable
fun TaskTypesScreen(
    selectedDevice: Device, // TODO: idealmente pasar solo deviceUuid y obtener por VM
    onTaskTypeClicked: (deviceUuid: String, taskTypeUid: Long) -> Unit
) {
    val tasksTypes = selectedDevice.taskTypes  // ya es una colección; no hace falta toList()

    Box(modifier = Modifier.fillMaxSize()) {

        if (tasksTypes.isEmpty()) {
            // Empty state simple (opcional)
            Text(
                text = "No task types available",
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = tasksTypes,
                key = { taskType -> "${selectedDevice.uuid}|taskType:${taskType.uid}" } // clave única y estable
            ) { task ->
                TaskTypeItem(
                    task = task,
                    onTaskTypeClicked = {
                        // Si es stateful y necesitas pedir sync al IoT, hazlo aquí antes de navegar
                        onTaskTypeClicked(selectedDevice.uuid, task.uid)
                    }
                )
            }
        }
    }
}
