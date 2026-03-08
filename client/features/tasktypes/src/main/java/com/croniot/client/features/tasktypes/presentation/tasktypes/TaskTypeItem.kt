package com.croniot.client.features.tasktypes.presentation.tasktypes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.croniot.client.core.models.TaskType
import com.croniot.client.core.models.isInstant
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TaskTypeItem(
    deviceUuid: String,
    taskType: TaskType,
    viewModel: TaskTypesViewModel,
    onTaskTypeClicked: () -> Unit,
) {

    val taskStateInfoFlow = viewModel.observeTaskTypeUpdates(
        deviceUuid = deviceUuid,
        taskType = taskType,
    ).collectAsStateWithLifecycle()
    val secondaryText by viewModel.getSecondaryText(deviceUuid, taskType).collectAsStateWithLifecycle()


    Card(
        onClick = onTaskTypeClicked,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if(taskType.isInstant()){
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)

                    ,
                )

                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.AddAlarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)

                    ,
                )

                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Text(
                    text = taskType.name,
                    style = MaterialTheme.typography.titleMedium
                )

                if(taskStateInfoFlow.value != null){
                    Text(
                        text = secondaryText,
                       // text = getSecondaryText(taskType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }


            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f)
            )
        }
    }
}


