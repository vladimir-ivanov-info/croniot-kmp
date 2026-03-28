package com.croniot.client.features.taskhistory.presentation

import Outcome
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase

class TaskStateInfoHistoryPagingSource(
    private val fetchTaskStateInfoHistoryUseCase: FetchTaskStateInfoHistoryUseCase,
    private val taskTypesRepository: TaskTypesRepository,
    private val deviceUuid: String,
    private val pageSize: Int,
) : PagingSource<Int, TaskHistoryItem>() {

    private val snapshotBefore: String = System.currentTimeMillis().toString()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TaskHistoryItem> {
        val offset = params.key ?: 0

        Log.d("Paging", "load() offset=$offset pageSize=$pageSize before=$snapshotBefore type=${params::class.simpleName}")

        return when (val outcome = fetchTaskStateInfoHistoryUseCase(deviceUuid, pageSize, offset, snapshotBefore)) {
            is Outcome.Ok -> {
                val items = outcome.value.map { entry ->
                    val typeName = taskTypesRepository.get(deviceUuid, entry.taskTypeUid)?.name ?: "Unknown"
                    TaskHistoryItem(
                        taskUid = entry.taskUid,
                        taskTypeUid = entry.taskTypeUid,
                        taskTypeName = typeName,
                        dateTime = entry.dateTime,
                        state = entry.state,
                        progress = entry.progress,
                        errorMessage = entry.errorMessage,
                    )
                }
                val nextKey = if (items.size < pageSize) null else offset + pageSize
                Log.d("Paging", "load() offset=$offset → received=${items.size} nextKey=$nextKey")
                LoadResult.Page(
                    data = items,
                    prevKey = if (offset == 0) null else (offset - pageSize).coerceAtLeast(0),
                    nextKey = nextKey,
                )
            }
            is Outcome.Err -> {
                Log.e("Paging", "load() offset=$offset → ERROR: ${outcome.error}")
                LoadResult.Error(RuntimeException(outcome.error.toString()))
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TaskHistoryItem>): Int? {
        return state.anchorPosition?.let { anchor ->
            val closestPage = state.closestPageToPosition(anchor)
            closestPage?.prevKey?.plus(closestPage.data.size)
                ?: closestPage?.nextKey?.minus(closestPage.data.size)
        }
    }
}
