package com.croniot.client.features.taskhistory.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.croniot.client.domain.models.TaskHistoryFilter
import com.croniot.client.domain.models.TaskType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskHistoryFilterSheet(
    filter: TaskHistoryFilter,
    availableTaskTypes: List<TaskType>,
    onAction: (TaskHistoryFilterAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(
                    onClick = { onAction(TaskHistoryFilterAction.ClearAllFilters) },
                    enabled = filter.isActive,
                    modifier = Modifier.alpha(if (filter.isActive) 1f else 0f),
                ) {
                    Text("Clear all")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task Types section
            if (availableTaskTypes.isNotEmpty()) {
                FilterSection(title = "Task Types") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        availableTaskTypes.forEach { taskType ->
                            val selected = taskType.uid in filter.taskTypeUids
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val newUids = if (selected) {
                                        filter.taskTypeUids - taskType.uid
                                    } else {
                                        filter.taskTypeUids + taskType.uid
                                    }
                                    onAction(TaskHistoryFilterAction.SetTaskTypeFilter(newUids))
                                },
                                label = { Text(taskType.name) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Date Range section
            FilterSection(title = "Date Range") {
                val dateLabel = buildDateRangeLabel(filter.dateFromMillis, filter.dateToMillis)
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(dateLabel)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            initialFromMillis = filter.dateFromMillis,
            initialToMillis = filter.dateToMillis,
            onConfirm = { fromMillis, toMillis ->
                onAction(TaskHistoryFilterAction.SetDateRange(fromMillis, toMillis))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialFromMillis: Long?,
    initialToMillis: Long?,
    onConfirm: (fromMillis: Long?, toMillis: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFromMillis,
        initialSelectedEndDateMillis = initialToMillis,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(state.selectedStartDateMillis, state.selectedEndDateMillis)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.height(480.dp),
        )
    }
}

private fun buildDateRangeLabel(fromMillis: Long?, toMillis: Long?): String {
    if (fromMillis == null && toMillis == null) return "Select dates"
    val zone = ZoneId.systemDefault()
    val fromLabel = fromMillis?.let {
        Instant.ofEpochMilli(it).atZone(zone).format(DATE_FORMATTER)
    }
    val toLabel = toMillis?.let {
        Instant.ofEpochMilli(it).atZone(zone).format(DATE_FORMATTER)
    }
    return when {
        fromLabel != null && toLabel != null -> "$fromLabel - $toLabel"
        fromLabel != null -> "From $fromLabel"
        else -> "Until $toLabel"
    }
}
