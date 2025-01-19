package com.croniot.android.features.device.features.sensors.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.croniot.android.features.login.controller.LoginController

@Composable
fun SensorsScreen(navController: NavController, viewModelSensors: ViewModelSensors){

    val sensorMap by viewModelSensors.sensorDataStateFlow.collectAsState()

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

            items(sensorMap.entries.toList()) { entry ->
                val sensor = entry.key
                val sensorDataFlow = entry.value
                SensorItem(sensor, sensorDataFlow)
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}