package com.croniot.client.features.taskhistory.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import croniot.models.TaskState

@Composable
fun TaskHistoryItemCard(item: TaskHistoryItem) {
    val icon: ImageVector
    val iconTint: Color

    when (item.state) {
        TaskState.CREATED.name -> {
            icon = Icons.Default.Schedule
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
        }
        TaskState.RUNNING.name -> {
            icon = Icons.Default.PlayArrow
            iconTint = MaterialTheme.colorScheme.primary
        }
        TaskState.COMPLETED.name -> {
            icon = Icons.Default.CheckCircle
            iconTint = MaterialTheme.colorScheme.primary
        }
        TaskState.ERROR.name -> {
            icon = Icons.Default.ErrorOutline
            iconTint = MaterialTheme.colorScheme.error
        }
        TaskState.PAUSED.name -> {
            icon = Icons.Default.Pause
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
        }
        else -> {
            icon = Icons.AutoMirrored.Filled.HelpOutline
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Card(
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.state,
                tint = iconTint,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.taskTypeName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}
