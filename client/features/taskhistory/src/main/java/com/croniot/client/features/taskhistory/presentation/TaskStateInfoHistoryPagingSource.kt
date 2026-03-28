package com.croniot.client.features.taskhistory.presentation

import Outcome
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase

class TaskStateInfoHistoryPagingSource(
    private val fetchTaskStateInfoHistoryUseCase: FetchTaskStateInfoHistoryUseCase,
    private val taskTypesRepository: TaskTypesRepository,
    private val deviceUuid: String,
) : PagingSource<Int, TaskHistoryItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TaskHistoryItem> {
        val offset = params.key ?: 0
        val limit = params.loadSize

        return when (val outcome = fetchTaskStateInfoHistoryUseCase(deviceUuid, limit, offset)) {
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
                LoadResult.Page(
                    data = items,
                    prevKey = if (offset == 0) null else offset - limit,
                    nextKey = if (items.size < limit) null else offset + limit,
                )
            }
            is Outcome.Err -> LoadResult.Error(RuntimeException(outcome.error.toString()))
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
