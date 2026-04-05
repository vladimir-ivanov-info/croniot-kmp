package com.croniot.client.features.taskhistory.presentation

import Outcome
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.croniot.client.domain.models.TaskHistoryFilter
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryCountUseCase
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

sealed interface TaskHistoryFilterAction {
    data class SetTaskTypeFilter(val taskTypeUids: Set<Long>) : TaskHistoryFilterAction
    data class SetDateRange(val fromMillis: Long?, val toMillis: Long?) : TaskHistoryFilterAction
    data object ClearAllFilters : TaskHistoryFilterAction
    data object ToggleFilterSheet : TaskHistoryFilterAction
}

@Immutable
data class TaskHistoryItem(
    val stateInfoId: Long,
    val taskUid: Long,
    val taskTypeUid: Long,
    val taskTypeName: String,
    val dateTime: ZonedDateTime,
    val state: String,
    val progress: Double,
    val errorMessage: String,
    val subtitle: String,
    val relativeTime: String,
    val time: String,
)

private const val PAGE_SIZE = 50
private const val MAX_NEW_ITEMS = 100
private const val MAX_SEEN_NEW_IDENTITIES = 5_000

private data class TaskHistoryItemIdentity(
    val stateInfoId: Long,
    val taskUid: Long,
    val taskTypeUid: Long,
    val dateTimeEpochMillis: Long,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)

private fun TaskHistoryItem.identity(): TaskHistoryItemIdentity =
    TaskHistoryItemIdentity(
        stateInfoId = stateInfoId,
        taskUid = taskUid,
        taskTypeUid = taskTypeUid,
        dateTimeEpochMillis = dateTime.toInstant().toEpochMilli(),
        state = state,
        progress = progress,
        errorMessage = errorMessage,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class TaskHistoryViewModel(
    private val fetchTaskStateInfoHistoryUseCase: FetchTaskStateInfoHistoryUseCase,
    private val fetchTaskStateInfoHistoryCountUseCase: FetchTaskStateInfoHistoryCountUseCase,
    private val tasksRepository: TasksRepository,
    private val taskTypesRepository: TaskTypesRepository,
) : ViewModel() {

    private val deviceUuidFlow = MutableStateFlow<String?>(null)
    private val snapshotBeforeFlow = MutableStateFlow<String?>(null)
    private val countRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _filterState = MutableStateFlow(TaskHistoryFilter.NONE)
    val filterState: StateFlow<TaskHistoryFilter> = _filterState.asStateFlow()

    private val _isFilterSheetVisible = MutableStateFlow(false)
    val isFilterSheetVisible: StateFlow<Boolean> = _isFilterSheetVisible.asStateFlow()

    private val _newItems = MutableStateFlow<List<TaskHistoryItem>>(emptyList())
    val newItems: StateFlow<List<TaskHistoryItem>> = _newItems.asStateFlow()
    private val _newEntriesSinceSnapshot = MutableStateFlow(0)
    val newEntriesSinceSnapshot: StateFlow<Int> = _newEntriesSinceSnapshot.asStateFlow()

    private val seenNewIdentitiesSinceSnapshot = LinkedHashSet<TaskHistoryItemIdentity>()

    private val _totalEntries = MutableStateFlow<Int?>(null)
    val totalEntries: StateFlow<Int?> = _totalEntries.asStateFlow()
    private var totalEntriesDeviceUuid: String? = null

    val pagingFlow: Flow<PagingData<TaskHistoryItem>> = combine(
        deviceUuidFlow,
        snapshotBeforeFlow,
        _filterState,
    ) { uuid, snapshotBefore, filter ->
        Triple(uuid, snapshotBefore, filter)
    }.flatMapLatest { (uuid, snapshotBefore, filter) ->
        if (uuid == null || snapshotBefore == null) return@flatMapLatest flowOf(PagingData.empty())

        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                prefetchDistance = PAGE_SIZE / 2,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                TaskStateInfoHistoryPagingSource(
                    fetchTaskStateInfoHistoryUseCase = fetchTaskStateInfoHistoryUseCase,
                    taskTypesRepository = taskTypesRepository,
                    deviceUuid = uuid,
                    snapshotBefore = snapshotBefore,
                    pageSize = PAGE_SIZE,
                    filter = filter,
                    onFirstPageLoaded = { countRefreshTrigger.tryEmit(Unit) },
                )
            },
        ).flow
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            deviceUuidFlow
                .filterNotNull()
                .distinctUntilChanged()
                .flatMapLatest { uuid ->
                    resetLiveNewItemsState()
                    tasksRepository.observeTaskStateInfoUpdates(uuid).map { event -> uuid to event }
                }
                .collect { (deviceUuid, event) ->
                    val currentFilter = _filterState.value
                    if (currentFilter.taskTypeUids.isNotEmpty() && event.key.taskTypeUid !in currentFilter.taskTypeUids) return@collect
                    val eventMillis = event.info.dateTime.toInstant().toEpochMilli()
                    val fromMillis = currentFilter.dateFromMillis
                    val toMillis = currentFilter.dateToMillis
                    if (fromMillis != null && eventMillis < fromMillis) return@collect
                    if (toMillis != null && eventMillis > toMillis) return@collect

                    val typeName = taskTypesRepository.get(deviceUuid, event.key.taskTypeUid)?.name ?: "Unknown"
                    val item = buildTaskHistoryItem(
                        stateInfoId = -1L,
                        taskUid = event.key.taskUid,
                        taskTypeUid = event.key.taskTypeUid,
                        taskTypeName = typeName,
                        dateTime = event.info.dateTime,
                        state = event.info.state,
                        progress = event.info.progress,
                        errorMessage = event.info.errorMessage,
                    )

                    if (!registerNewIdentity(item.identity())) return@collect

                    _newEntriesSinceSnapshot.update { it + 1 }
                    _newItems.update { current ->
                        buildList(capacity = MAX_NEW_ITEMS) {
                            add(item)
                            current.forEach { existing ->
                                if (size >= MAX_NEW_ITEMS) return@forEach
                                add(existing)
                            }
                        }
                    }
                }
        }

        viewModelScope.launch {
            val stateFlow = combine(deviceUuidFlow, snapshotBeforeFlow, _filterState) { uuid, snapshotBefore, filter ->
                Triple(uuid, snapshotBefore, filter)
            }
            val refreshFlow = countRefreshTrigger.map {
                Triple(deviceUuidFlow.value, snapshotBeforeFlow.value, _filterState.value)
            }
            merge(stateFlow, refreshFlow)
                .collectLatest { (uuid, snapshotBefore, filter) ->
                    if (uuid == null || snapshotBefore == null) {
                        _totalEntries.value = null
                        totalEntriesDeviceUuid = null
                        return@collectLatest
                    }

                    if (totalEntriesDeviceUuid != uuid) {
                        _totalEntries.value = null
                        totalEntriesDeviceUuid = uuid
                    }
                    when (val result = fetchTaskStateInfoHistoryCountUseCase(uuid, snapshotBefore, filter = filter)) {
                        is Outcome.Ok -> _totalEntries.value = result.value
                        is Outcome.Err -> Unit
                    }
                }
        }
    }

    fun initialize(deviceUuid: String) {
        val currentDeviceUuid = deviceUuidFlow.value
        val hasSnapshot = snapshotBeforeFlow.value != null

        if (currentDeviceUuid != deviceUuid) {
            deviceUuidFlow.value = deviceUuid
            snapshotBeforeFlow.value = System.currentTimeMillis().toString()
            resetLiveNewItemsState()
            return
        }

        if (!hasSnapshot) {
            snapshotBeforeFlow.value = System.currentTimeMillis().toString()
            resetLiveNewItemsState()
        }
    }

    private fun registerNewIdentity(identity: TaskHistoryItemIdentity): Boolean {
        if (!seenNewIdentitiesSinceSnapshot.add(identity)) return false

        if (seenNewIdentitiesSinceSnapshot.size > MAX_SEEN_NEW_IDENTITIES) {
            val oldest = seenNewIdentitiesSinceSnapshot.iterator()
            if (oldest.hasNext()) {
                oldest.next()
                oldest.remove()
            }
        }
        return true
    }

    fun onFilterAction(action: TaskHistoryFilterAction) {
        when (action) {
            is TaskHistoryFilterAction.SetTaskTypeFilter -> applyFilter(
                _filterState.value.copy(taskTypeUids = action.taskTypeUids),
            )
            is TaskHistoryFilterAction.SetDateRange -> applyFilter(
                _filterState.value.copy(dateFromMillis = action.fromMillis, dateToMillis = action.toMillis),
            )
            TaskHistoryFilterAction.ClearAllFilters -> applyFilter(TaskHistoryFilter.NONE)
            TaskHistoryFilterAction.ToggleFilterSheet -> {
                _isFilterSheetVisible.update { !it }
            }
        }
    }

    private fun applyFilter(newFilter: TaskHistoryFilter) {
        if (newFilter == _filterState.value) return
        _filterState.value = newFilter
        resetLiveNewItemsState()
        snapshotBeforeFlow.value = System.currentTimeMillis().toString()
    }

    private fun resetLiveNewItemsState() {
        _newItems.value = emptyList()
        _newEntriesSinceSnapshot.value = 0
        seenNewIdentitiesSinceSnapshot.clear()
    }
}
