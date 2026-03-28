package com.croniot.client.features.taskhistory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean

data class TaskHistoryItem(
    val taskUid: Long,
    val taskTypeUid: Long,
    val taskTypeName: String,
    val dateTime: ZonedDateTime,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)

private const val PAGE_SIZE = 30

class TaskHistoryViewModel(
    private val fetchTaskStateInfoHistoryUseCase: FetchTaskStateInfoHistoryUseCase,
    private val tasksRepository: TasksRepository,
    private val taskTypesRepository: TaskTypesRepository,
) : ViewModel() {

    private val deviceUuidFlow = MutableStateFlow<String?>(null)
    private val initialized = AtomicBoolean(false)

    private var currentPagingSource: TaskStateInfoHistoryPagingSource? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlow: Flow<PagingData<TaskHistoryItem>> = deviceUuidFlow.flatMapLatest { uuid ->
        if (uuid == null) return@flatMapLatest flowOf(PagingData.empty())

        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                prefetchDistance = PAGE_SIZE / 2,
            ),
            pagingSourceFactory = {
                TaskStateInfoHistoryPagingSource(
                    fetchTaskStateInfoHistoryUseCase = fetchTaskStateInfoHistoryUseCase,
                    taskTypesRepository = taskTypesRepository,
                    deviceUuid = uuid,
                ).also { currentPagingSource = it }
            },
        ).flow
    }.cachedIn(viewModelScope)

    fun initialize(deviceUuid: String) {
        if (!initialized.compareAndSet(false, true)) return

        deviceUuidFlow.value = deviceUuid

        viewModelScope.launch {
            tasksRepository.observeTaskStateInfoUpdates(deviceUuid).collect {
                currentPagingSource?.invalidate()
            }
        }
    }
}
