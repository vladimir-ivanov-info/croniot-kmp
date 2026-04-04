package com.croniot.client.features.taskhistory.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.croniot.client.domain.models.Device
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TaskHistoryScreen(
    selectedDevice: Device,
    viewModel: TaskHistoryViewModel = koinViewModel(),
) {
    LaunchedEffect(selectedDevice.uuid) {
        viewModel.initialize(selectedDevice.uuid)
    }

    val newItems by viewModel.newItems.collectAsStateWithLifecycle()
    val newEntriesSinceSnapshot by viewModel.newEntriesSinceSnapshot.collectAsStateWithLifecycle()
    val totalEntriesBySnapshot by viewModel.totalEntries.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    val loadedCount = newItems.size + pagingItems.itemCount
    val totalEntries = totalEntriesBySnapshot?.let { snapshotTotal ->
        maxOf(loadedCount, snapshotTotal + newEntriesSinceSnapshot)
    }
    val totalLabel = totalEntries?.toString() ?: "..."
    val currentPosition = if (loadedCount == 0) 0 else (listState.firstVisibleItemIndex + 1)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            pagingItems.loadState.refresh is LoadState.Loading && newItems.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            pagingItems.loadState.refresh is LoadState.Error && newItems.isEmpty() -> {
                val error = (pagingItems.loadState.refresh as LoadState.Error).error
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Error loading history",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = error.localizedMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            loadedCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    )
                    Text(
                        text = "No tasks yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = newItems,
                            key = { "new_${it.taskTypeUid}_${it.taskUid}_${it.dateTime}" },
                            contentType = { "task_history_item" },
                        ) { item ->
                            TaskHistoryItemCard(item = item)
                        }

                        items(
                            count = pagingItems.itemCount,
                            key = { index ->
                                pagingItems.peek(index)?.let { item ->
                                    if (item.stateInfoId > 0L) {
                                        "paged_${item.stateInfoId}"
                                    } else {
                                        "paged_${item.taskTypeUid}_${item.taskUid}_${item.dateTime.toInstant().toEpochMilli()}_${item.state}_$index"
                                    }
                                } ?: "paged_placeholder_$index"
                            },
                            contentType = { index ->
                                if (pagingItems.peek(index) == null) "task_history_placeholder" else "task_history_item"
                            },
                        ) { index ->
                            pagingItems[index]?.let { item ->
                                TaskHistoryItemCard(item = item)
                            }
                        }

                        if (pagingItems.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }

                    EntriesCountBar(
                        currentCount = currentPosition.toString(),
                        totalCount = totalLabel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EntriesCountBar(
    currentCount: String,
    totalCount: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountChip(value = currentCount)
            Text(
                text = "/",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            CountChip(value = totalCount)
            Text(
                text = "entries",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CountChip(value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .widthIn(min = 72.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
