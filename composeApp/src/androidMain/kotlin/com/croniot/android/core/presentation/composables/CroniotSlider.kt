package com.croniot.android.core.presentation.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.croniot.android.domain.model.ParameterTask
import com.croniot.android.features.device.features.tasktypes.ViewModelTaskTypes
import com.croniot.android.features.device.features.tasktypes.formatNumber
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun CroniotSlider(currentParameter: ParameterTask, index: Int, viewModelTaskTypes: ViewModelTaskTypes) {
    val minValue = currentParameter.constraints["minValue"]
    val maxValue = currentParameter.constraints["maxValue"]

    val sliderValue = viewModelTaskTypes.parametersValues.toList()[index].second.collectAsState()
    var sliderValueStr = sliderValue.value

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val steps = maxValue!!.toInt()

        sliderValueStr = BigDecimal(sliderValueStr.toDouble()).setScale(1, RoundingMode.HALF_UP).toString()

        LabeledSlider(
            parameter = currentParameter,
            value = sliderValueStr.toFloat(),
            onValueChange = {
                viewModelTaskTypes.updateParameter(currentParameter.uid, it.toString())
            },
            valueRange = minValue!!.toFloat()..maxValue!!.toFloat(),
            steps = steps,
            minValue = minValue!!.toFloat(),
            maxValue = maxValue!!.toFloat(),
            minValueLabel = minValue,
            maxValueLabel = maxValue,
            currentValueLabel = "abc",
            constraints = currentParameter.constraints,
        )
    }
}

@Composable
fun LabeledSlider(
    parameter: ParameterTask,
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
    constraints: MutableMap<String, String>,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "· ${parameter.name}:", fontWeight = FontWeight.Bold)
            Text(text = formatNumber(value, constraints), fontWeight = FontWeight.Bold)
            Text(text = parameter.unit, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            // valueRange = minValue - 1..maxValue-1,
            valueRange = valueRange,
            // steps = (maxValue - minValue).toInt() - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
