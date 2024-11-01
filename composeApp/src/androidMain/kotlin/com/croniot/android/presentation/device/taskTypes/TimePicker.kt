package com.croniot.android.presentation.device.taskTypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import croniot.models.dto.ParameterTaskDto
import kotlinx.coroutines.delay
import java.util.Calendar



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(
    parameter: ParameterTaskDto,
    viewModelTask: ViewModelTaskTypes
) {
    val currentTime = Calendar.getInstance()

    // Set up the mutable time picker state with initial hour and minute
    val timePickerState = remember {
        mutableStateOf(
            TimePickerState(
                initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
                initialMinute = currentTime.get(Calendar.MINUTE),
                is24Hour = true
            )
        )
    }

    LaunchedEffect(timePickerState.value.hour, timePickerState.value.minute) {
        val newValue = "${timePickerState.value.hour}:${timePickerState.value.minute}"
        viewModelTask.updateParameter(parameter.uid, newValue)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),  // Ensure Column takes full width
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(modifier = Modifier
            .fillMaxWidth()
            ,
            horizontalArrangement = Arrangement.spacedBy(4.dp)){

            Text(text = "· ${parameter.name}:", fontWeight = FontWeight.Bold)
            Text(text = "${timePickerState.value.hour}:${timePickerState.value.minute}", fontWeight = FontWeight.Bold)
            Text(text = "h", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.padding(bottom = 4.dp))

        androidx.compose.material3.TimePicker(
            state = timePickerState.value,
        )
    }
}