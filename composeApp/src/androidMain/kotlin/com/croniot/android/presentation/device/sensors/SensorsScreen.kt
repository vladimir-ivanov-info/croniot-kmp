package com.croniot.android.presentation.device.sensors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SensorsScreen(viewModelSensors: ViewModelSensors){
    val sensorDataMap = viewModelSensors.map

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