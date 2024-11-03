package com.croniot.android.presentation.device.sensors

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.croniot.android.ui.PerformanceChart
import com.croniot.android.ui.UtilUi
import croniot.models.SensorData
import croniot.models.dto.SensorDto
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SensorItem(sensor: SensorDto, sensorDataFlow: MutableStateFlow<SensorData>) {

    val sensorDataState = sensorDataFlow.collectAsState()
    val sensorData = sensorDataState.value

    val sensorName = sensor.parameters.first().name //TODO adapt for when there are many parameters in a sensor
    val sensorUnit = sensor.parameters.first().unit //TODO adapt for when there are many parameters in a sensor
    val sensorDataValue = sensorData.value

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        elevation = CardDefaults.elevatedCardElevation()

    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                //.background(Colors.primary),
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = sensorDataValue + " " + sensorUnit,

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

            val list = remember { mutableListOf<Float>() }

            if(!sensorData.value.equals("empty_value")){
                try{
                    val data = sensorData.value.toFloat()
                    list.add(data)

                    if(list.size > 100){
                        list.removeFirst()
                    }

                } catch (e: Throwable){
                    println()
                }
            }
            PerformanceChart(sensor, modifier = Modifier, list)
        }
    }
}