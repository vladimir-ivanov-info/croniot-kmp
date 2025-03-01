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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.croniot.android.app.Global
import com.croniot.android.core.presentation.composables.PerformanceChart
import com.croniot.android.core.presentation.util.UtilUi
import com.croniot.android.domain.model.SensorData
import com.croniot.android.domain.model.SensorType
import com.croniot.android.features.device.features.tasktypes.ViewModelTaskTypes
import java.time.ZonedDateTime

@Composable
fun SensorItem(sensor: SensorType, viewModelSensors: ViewModelSensors) {
    val deviceUuid = Global.selectedDevice!!.uuid
    val sensorDataFlow = viewModelSensors.observeLiveSensorData(sensor.uid, deviceUuid)

    val sensorData by sensorDataFlow.collectAsState(
        initial = SensorData(sensor.uid.toString(), sensor.uid, ViewModelTaskTypes.PARAMETER_VALUE_UNDEFINED, ZonedDateTime.now()),
    )

    val chartValues = remember { mutableStateListOf<SensorData>() }

    LaunchedEffect(Unit) {
        val latestData = viewModelSensors.getInitialChartData(sensor.uid, deviceUuid)
        chartValues.clear()
        chartValues.addAll(latestData.reversed())
    }

    if (chartValues.isEmpty() || chartValues.last().dateTime != sensorData.dateTime) {
        if (sensorData.value != ViewModelTaskTypes.PARAMETER_VALUE_UNDEFINED) {
            chartValues.add(sensorData)
        }
        if (chartValues.size > 50) chartValues.removeAt(0)
    }

    val sensorName = sensor.parameters.first().name
    val sensorUnit = sensor.parameters.first().unit

    var sensorDataValueUi = sensorData.value

    if (sensorDataValueUi == ViewModelTaskTypes.PARAMETER_VALUE_UNDEFINED) {
        sensorDataValueUi = ""
    }

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
                text = "$sensorDataValueUi $sensorUnit",
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

            if (sensor.uid.toInt() != 45) { // TODO we skip the "System time" sensor (sensorId = 45)
                val latestSensorValues = chartValues.map { it.value.toFloat() }
                PerformanceChart(sensor, modifier = Modifier, latestSensorValues)
            }
        }
    }
}
