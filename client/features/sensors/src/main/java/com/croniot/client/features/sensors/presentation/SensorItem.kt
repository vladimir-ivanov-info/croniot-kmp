package com.croniot.client.features.sensors.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.croniot.client.core.util.DateTimeUtil
import com.croniot.client.presentation.PerformanceChart
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SensorItem(
    sensorType: SensorType,
    initialSensorData: List<SensorData>,
    sensorDataFlow: StateFlow<SensorData>,
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
    val sensorUnit = firstParam?.unit ?: ""
    val hasData = sensorData.value != Constants.PARAMETER_VALUE_UNDEFINED

    val valueText = remember(sensorData.value, sensorUnit) {
        if (sensorData.value == Constants.PARAMETER_VALUE_UNDEFINED) ""
        else "${sensorData.value} $sensorUnit"
    }

    val timestampText = remember(sensorData.timeStamp, sensorData.value) {
        if (sensorData.value == Constants.PARAMETER_VALUE_UNDEFINED) ""
        else DateTimeUtil.formatRelativeTime(sensorData.timeStamp)
    }

    val latestSensorValues by remember(chartValues) {
        derivedStateOf { chartValues.mapNotNull { it.value.toFloatOrNull() } }
    }

    val spokenSensorItem = remember(sensorType.name, valueText, hasData) {
        "${sensorType.name}: ${if (hasData) valueText else "no data"}"
    }

    val chartable = sensorType.isChartable()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .height(if (chartable) 140.dp else 80.dp)
            .semantics(mergeDescendants = true) { contentDescription = spokenSensorItem },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: sensor name (label) + current value + timestamp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sensorType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                    if (hasData) {
                        Text(
                            text = valueText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    } else {
                        Text(
                            text = "Waiting for data…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }
                //TODO for now
                /*if (hasData) {
                    Text(
                        text = timestampText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }*/
            }

            // Chart below the header, no overlap with text
            if (chartable) {
                PerformanceChart(
                    sensorType = sensorType,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .clearAndSetSemantics { },
                    list = latestSensorValues,
                )
            }
        }
    }
}
