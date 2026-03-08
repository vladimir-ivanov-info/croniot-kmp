package com.croniot.client.features.sensors.presentation

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.core.config.Constants
import com.croniot.client.core.models.SensorData
import com.croniot.client.core.models.SensorType
import com.croniot.client.core.models.isChartable
import com.croniot.client.presentation.PerformanceChart
import com.croniot.client.presentation.constants.UtilUi
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SensorItem(
    sensorType: SensorType,
    initialSensorData: List<SensorData>, //TODO populate flow with initial data in the VM instead of passing initial data to composable = less arguments
    sensorDataFlow: StateFlow<SensorData>
) {
    val sensorData by sensorDataFlow.collectAsStateWithLifecycle()

    val chartValues = remember { mutableStateListOf<SensorData>() }

    LaunchedEffect(Unit) {
        chartValues.clear()
        chartValues.addAll(initialSensorData)
    }

    LaunchedEffect(sensorData.timeStamp) {
        if (sensorData.value != Constants.PARAMETER_VALUE_UNDEFINED) {
            val lastTime = chartValues.lastOrNull()?.timeStamp
            if (lastTime != sensorData.timeStamp) {
                if (chartValues.size >= 50) {
                    val trimmed = chartValues.drop(1) + sensorData
                    chartValues.clear()
                    chartValues.addAll(trimmed)
                } else {
                    chartValues.add(sensorData)
                }
            }
        }
    }

    val firstParam = remember(sensorType.parameters) { sensorType.parameters.firstOrNull() }
    val sensorName = firstParam?.name ?: ""
    val sensorUnit = firstParam?.unit ?: ""

    val valueText by remember(sensorData.value, sensorUnit) {
        derivedStateOf {
            if (sensorData.value == Constants.PARAMETER_VALUE_UNDEFINED) "-no data history-" else "${sensorData.value} $sensorUnit"
        }
    }

    val spokenSensorItem = remember(sensorName, valueText) {
        "$sensorName: $valueText"
    }

    val latestSensorValues by remember(chartValues) {
        derivedStateOf { chartValues.mapNotNull { it.value.toFloatOrNull() } }
    }

    // 5) UI liviana: evita trabajo extra en layout/medición (altura fija, sin fillMaxSize en el Box si no hace falta)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics(mergeDescendants = true) {
                    contentDescription = spokenSensorItem
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = valueText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.Center)
                    .clearAndSetSemantics { },
            )

            Text(
                text = sensorName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = UtilUi.TEXT_SIZE_3,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.BottomCenter)
                    .clearAndSetSemantics { },
            )

            if (sensorType.isChartable()) {
                PerformanceChart(
                    sensorType = sensorType,
                    modifier = Modifier.clearAndSetSemantics { }, // TODO poner un tamaño fijo si puedes para ahorrar layout passes
                    list = latestSensorValues,
                )
            }
        }
    }
}

