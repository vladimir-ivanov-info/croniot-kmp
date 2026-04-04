package com.croniot.client.features.sensors.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.domain.models.Device
import org.koin.androidx.compose.koinViewModel

@Composable
fun SensorsScreen(
    selectedDevice: Device,
    sensorsViewModel: SensorsViewModel = koinViewModel(),
) {
    val selectedDeviceUuid = selectedDevice.uuid

    val sensorTypes = selectedDevice.sensorTypes

    LaunchedEffect(selectedDeviceUuid, sensorTypes) {
        sensorsViewModel.loadAllInitialData(selectedDeviceUuid, sensorTypes)
    }
    val sensorsInitialData by sensorsViewModel.sensorsInitialData.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }
            items(
                items = sensorTypes,
                key = { it.uid },
                contentType = { "sensorItem" }, // ayuda al recycler
            ) { sensorType ->

                val sensorTypeUid = sensorType.uid

                val isLoaded = sensorsInitialData.containsKey(sensorType.uid)
                val initialData = sensorsInitialData[sensorTypeUid] ?: emptyList()

                if (isLoaded) {
                    val sensorDataFlow = remember(sensorTypeUid, selectedDeviceUuid) {
                        sensorsViewModel.listenSensorData(sensorTypeUid, selectedDeviceUuid)
                    }
                    SensorItem(
                        sensorType = sensorType,
                        initialSensorData = initialData,
                        sensorDataFlow = sensorDataFlow,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
