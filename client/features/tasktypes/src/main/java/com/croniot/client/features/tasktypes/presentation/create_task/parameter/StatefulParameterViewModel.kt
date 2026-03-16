package com.croniot.client.features.tasktypes.presentation.create_task.parameter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.usecases.RequestTaskStateInfoSyncUseCase
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import java.time.ZonedDateTime

class StatefulParameterViewModel(
    private val tasksRepository: TasksRepository,
    private val requestTaskStateInfoSyncUseCase: RequestTaskStateInfoSyncUseCase,
) : ViewModel() {

    private val _statefulTaskInfoParameterSynced = MutableStateFlow(false)
    val statefulTaskInfoParameterSynced: StateFlow<Boolean> get() = _statefulTaskInfoParameterSynced

    fun initialize(deviceUuid: String, taskTypeUid: Long) = launchInVmScope {
        flow {
            while (currentCoroutineContext().isActive) {
                emit(tasksRepository.getLatestTaskStateInfoEmittedByIoT(deviceUuid, taskTypeUid))
                delay(1000)
            }
        }
        .map { latestInfo -> computeSyncState(latestInfo?.dateTime) }
        //.distinctUntilChanged()
        .onEach { (isSynced, shouldRequestSync) ->
            _statefulTaskInfoParameterSynced.value = isSynced
            if (shouldRequestSync) {
                requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeUid)
            }
        }
        .launchIn(viewModelScope)

        /*while (isActive) {
            val latestTaskStateInfoFromIoT = tasksRepository.getLatestTaskStateInfoEmittedByIoT(deviceUuid, taskTypeUid)
            if (latestTaskStateInfoFromIoT != null) {
                val now = ZonedDateTime.now()
                val latestDateTime = latestTaskStateInfoFromIoT.dateTime

                val isSynced = !now.isAfter(latestDateTime.plusSeconds(5))
                val shouldRequestSync = now.isAfter(latestDateTime.plusSeconds(3))

                if (shouldRequestSync) {
                    requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeUid)
                }

                _statefulTaskInfoParameterSynced.value = isSynced
            } else {
                _statefulTaskInfoParameterSynced.value = false
                requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeUid)
            }

            delay(1000)
        }*/
    }

    private fun computeSyncState(latestDateTime: ZonedDateTime?): SyncState {
        if (latestDateTime == null) return SyncState(isSynced = false, shouldRequestSync = true)
        val now = ZonedDateTime.now()
        return SyncState(
            isSynced = !now.isAfter(latestDateTime.plusSeconds(5)),
            shouldRequestSync = now.isAfter(latestDateTime.plusSeconds(3)),
        )
    }

    private data class SyncState(val isSynced: Boolean, val shouldRequestSync: Boolean)
}