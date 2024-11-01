package com.croniot.android.presentation.device.taskTypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import croniot.models.dto.ParameterTaskDto

@Composable
fun LabeledSlider(
    parameter: ParameterTaskDto,
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    minValue: Float,
    maxValue: Float,
    minValueLabel: String,
    maxValueLabel: String,
    currentValueLabel: String,
    constraints: MutableMap<String, String>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            ,
            horizontalArrangement = Arrangement.spacedBy(4.dp)){

            Text(text = "· ${parameter.name}:", fontWeight = FontWeight.Bold)
            Text(text = formatNumber(value, constraints), fontWeight = FontWeight.Bold)
            Text(text = parameter.unit, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            //valueRange = minValue - 1..maxValue-1,
            valueRange = valueRange,
            // steps = (maxValue - minValue).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )
    }

}