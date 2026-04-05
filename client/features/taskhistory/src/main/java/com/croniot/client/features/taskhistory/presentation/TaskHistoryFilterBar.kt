package com.croniot.client.features.taskhistory.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.croniot.client.domain.models.TaskHistoryFilter
import com.croniot.client.domain.models.TaskType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SHORT_DATE = DateTimeFormatter.ofPattern("dd MMM")

@Composable
fun TaskHistoryFilterBar(
    filter: TaskHistoryFilter,
    availableTaskTypes: List<TaskType>,
    onAction: (TaskHistoryFilterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val activeCount = countActiveFilters(filter)

        IconButton(onClick = { onAction(TaskHistoryFilterAction.ToggleFilterSheet) }) {
            if (activeCount > 0) {
                BadgedBox(badge = {
                    Badge { Text(activeCount.toString()) }
                }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                )
            }
        }

        if (filter.isActive) {
            LazyRow(
                contentPadding = PaddingValues(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (filter.taskTypeUids.isNotEmpty()) {
                    val selectedNames = availableTaskTypes
                        .filter { it.uid in filter.taskTypeUids }
                        .map { it.name }
                    val label = if (selectedNames.size <= 2) {
                        selectedNames.joinToString(", ")
                    } else {
                        "${selectedNames.first()} +${selectedNames.size - 1}"
                    }
                    item(key = "tasktype_chip") {
                        DismissableChip(
                            label = label,
                            onClick = { onAction(TaskHistoryFilterAction.ToggleFilterSheet) },
                            onDismiss = { onAction(TaskHistoryFilterAction.SetTaskTypeFilter(emptySet())) },
                        )
                    }
                }

                if (filter.dateFromMillis != null || filter.dateToMillis != null) {
                    item(key = "date_chip") {
                        DismissableChip(
                            label = buildChipDateLabel(filter.dateFromMillis, filter.dateToMillis),
                            onClick = { onAction(TaskHistoryFilterAction.ToggleFilterSheet) },
                            onDismiss = { onAction(TaskHistoryFilterAction.SetDateRange(null, null)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DismissableChip(
    label: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    InputChip(
        selected = true,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove filter",
                modifier = Modifier
                    .size(InputChipDefaults.AvatarSize)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDismiss),
            )
        },
    )
}

private fun countActiveFilters(filter: TaskHistoryFilter): Int {
    var count = 0
    if (filter.taskTypeUids.isNotEmpty()) count++
    if (filter.dateFromMillis != null || filter.dateToMillis != null) count++
    return count
}

private fun buildChipDateLabel(fromMillis: Long?, toMillis: Long?): String {
    val zone = ZoneId.systemDefault()
    val from = fromMillis?.let { Instant.ofEpochMilli(it).atZone(zone).format(SHORT_DATE) }
    val to = toMillis?.let { Instant.ofEpochMilli(it).atZone(zone).format(SHORT_DATE) }
    return when {
        from != null && to != null -> "$from - $to"
        from != null -> "From $from"
        else -> "Until $to"
    }
}
