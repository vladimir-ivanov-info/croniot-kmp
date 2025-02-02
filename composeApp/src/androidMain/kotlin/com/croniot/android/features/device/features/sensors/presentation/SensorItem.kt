package com.croniot.android.features.device.features.sensors.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.croniot.android.app.Global
import com.croniot.android.core.presentation.composables.PerformanceChart
import com.croniot.android.core.presentation.util.UtilUi
import croniot.models.dto.SensorDataDto
import croniot.models.dto.SensorTypeDto
import java.time.ZonedDateTime

@Composable
fun SensorItem(sensor: SensorTypeDto, viewModelSensors: ViewModelSensors) {
    val initialChartData by produceState(initialValue = emptyList<SensorDataDto>()) {
        value = viewModelSensors.getInitialChartData(sensor.uid, Global.selectedDevice!!.uuid)
    }

    val sensorData by viewModelSensors.observeLiveSensorData(sensor.uid, Global.selectedDevice!!.uuid)
        .collectAsState(initial = SensorDataDto(sensor.uid.toString(), sensor.uid, "0", ZonedDateTime.now()))
    val chartValues = remember { mutableStateListOf<SensorDataDto>().apply { addAll(initialChartData) } }

    LaunchedEffect(sensorData) {
        if (chartValues.isEmpty() || chartValues.last().timestamp != sensorData.timestamp) {
            chartValues.add(sensorData)
            if (chartValues.size > 50) chartValues.removeAt(0) // Keep only the last 50 values
        }
    }

    val sensorName = sensor.parameters.first().name // TODO adapt for when there are many parameters in a sensor
    val sensorUnit = sensor.parameters.first().unit // TODO adapt for when there are many parameters in a sensor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        elevation = CardDefaults.elevatedCardElevation(),

    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = sensorData.value + " " + sensorUnit,
                fontWeight = FontWeight.Bold,
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

            if(sensor.uid.toInt() != 45){ //TODO adapt later, 45 is SensorCurrentTIme
                val latestSensorValues = chartValues.map { it.value.toFloat() }
                PerformanceChart(sensor, modifier = Modifier, latestSensorValues)
            }

        }
    }
}
