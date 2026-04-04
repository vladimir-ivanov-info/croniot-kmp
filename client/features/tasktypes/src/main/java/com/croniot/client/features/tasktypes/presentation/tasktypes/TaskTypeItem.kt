package com.croniot.client.features.tasktypes.presentation.tasktypes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.models.isInstant
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TaskTypeItem(
    taskType: TaskType,
    secondaryTextFlow: StateFlow<String>,
    onTaskTypeClicked: () -> Unit,
) {
    val secondaryText by secondaryTextFlow.collectAsStateWithLifecycle()

    Card(
        onClick = onTaskTypeClicked,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
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
            Icon(
                imageVector = if (taskType.isInstant()) Icons.Default.Bolt else Icons.Default.AddAlarm,
                contentDescription = if (taskType.isInstant()) "Instant task" else "Scheduled task",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = taskType.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = secondaryText.ifEmpty { "\u00A0" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (secondaryText.isNotEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0f)
                    },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
    }
}
