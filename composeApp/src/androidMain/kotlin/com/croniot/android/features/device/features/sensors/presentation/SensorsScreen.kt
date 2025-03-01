package com.croniot.android.features.device.features.sensors.presentation

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
import com.croniot.android.core.data.entities.SensorDataRealm
import croniot.models.dto.SensorDataDto
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun SensorDataRealm.toSensorDataDto(): SensorDataDto {
    return SensorDataDto(
        deviceUuid = this.deviceUuid,
        sensorTypeUid = this.sensorTypeUid,
        value = this.value,
        timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneOffset.UTC), // TODO store correct time zone anywhere
    )
}

@Composable
fun SensorsScreen(navController: NavController, viewModelSensors: ViewModelSensors) {
    val sensorTypes = Global.selectedDevice!!.sensors // TODO

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

            items(sensorTypes.toList()) { sensorType ->
                SensorItem(sensorType, viewModelSensors)
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
