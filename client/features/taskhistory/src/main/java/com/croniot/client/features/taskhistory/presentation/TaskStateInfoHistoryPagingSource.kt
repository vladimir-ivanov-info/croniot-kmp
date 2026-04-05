package com.croniot.client.features.taskhistory.presentation

import Outcome
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.croniot.client.domain.models.TaskHistoryFilter
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase

data class TaskHistoryCursor(
    val before: String,
    val beforeId: Long,
)

class TaskStateInfoHistoryPagingSource(
    private val fetchTaskStateInfoHistoryUseCase: FetchTaskStateInfoHistoryUseCase,
    private val taskTypesRepository: TaskTypesRepository,
    private val deviceUuid: String,
    private val snapshotBefore: String,
    private val pageSize: Int,
    private val filter: TaskHistoryFilter = TaskHistoryFilter.NONE,
    private val onFirstPageLoaded: () -> Unit = {},
) : PagingSource<TaskHistoryCursor, TaskHistoryItem>() {

    private var firstPageNotified = false

    override suspend fun load(params: LoadParams<TaskHistoryCursor>): LoadResult<TaskHistoryCursor, TaskHistoryItem> {
        val cursor = params.key ?: TaskHistoryCursor(
            before = snapshotBefore,
            beforeId = Long.MAX_VALUE,
        )

        Log.d(
            "Paging",
            "load() before=${cursor.before} beforeId=${cursor.beforeId} pageSize=$pageSize type=${params::class.simpleName}",
        )

        return when (val outcome = fetchTaskStateInfoHistoryUseCase(deviceUuid, pageSize, cursor.before, cursor.beforeId, filter)) {
            is Outcome.Ok -> {
                val items = outcome.value.map { entry ->
                    val typeName = taskTypesRepository.get(deviceUuid, entry.taskTypeUid)?.name ?: "Unknown"
                    buildTaskHistoryItem(
                        stateInfoId = entry.stateInfoId,
                        taskUid = entry.taskUid,
                        taskTypeUid = entry.taskTypeUid,
                        taskTypeName = typeName,
                        dateTime = entry.dateTime,
                        state = entry.state,
                        progress = entry.progress,
                        errorMessage = entry.errorMessage,
                    )
                }
                val nextKey = if (items.size < pageSize || items.isEmpty()) {
                    null
                } else {
                    val last = items.last()
                    val candidate = TaskHistoryCursor(
                        before = last.dateTime.toInstant().toEpochMilli().toString(),
                        beforeId = if (last.stateInfoId > 0L) last.stateInfoId else Long.MAX_VALUE,
                    )
                    if (candidate == cursor) null else candidate
                }
                Log.d(
                    "Paging",
                    "load() before=${cursor.before} beforeId=${cursor.beforeId} -> received=${items.size} nextKey=$nextKey",
                )
                if (!firstPageNotified) {
                    firstPageNotified = true
                    onFirstPageLoaded()
                }
                LoadResult.Page(
                    data = items,
                    prevKey = null,
                    nextKey = nextKey,
                )
            }
            is Outcome.Err -> {
                Log.e("Paging", "load() before=${cursor.before} beforeId=${cursor.beforeId} -> ERROR: ${outcome.error}")
                LoadResult.Error(RuntimeException(outcome.error.toString()))
            }
        }
    }

    override fun getRefreshKey(state: PagingState<TaskHistoryCursor, TaskHistoryItem>): TaskHistoryCursor? {
        return null
    }
}
