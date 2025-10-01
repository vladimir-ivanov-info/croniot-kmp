package com.croniot.android.features.device.features.sensors.presentation

import androidx.compose.foundation.layout.Arrangement
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
import com.croniot.client.core.models.Device
import com.croniot.client.features.sensors.presentation.SensorItem
import com.croniot.client.features.sensors.presentation.SensorsViewModel
import org.koin.androidx.compose.koinViewModel

/*fun SensorDataRealm.toSensorDataDto(): SensorDataDto {
    return SensorDataDto(
        deviceUuid = this.deviceUuid,
        sensorTypeUid = this.sensorTypeUid,
        value = this.value,
        timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeStampMillis), ZoneOffset.UTC) //TODO store correct time zone anywhere
    )
}*/

@Composable
fun SensorsScreen(selectedDevice: Device,
                  navController: NavController,
                  sensorsViewModel: SensorsViewModel = koinViewModel()
) {
    val sensorTypes = selectedDevice.sensorTypes

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ,
        ) {
            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }

            items(
                items = sensorTypes,
                key = { it.uid },
                contentType = { "sensorItem" }   // ayuda al recycler
            ) { sensorType ->
                SensorItem(sensorType, selectedDevice, sensorsViewModel)
            }

            item {
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
