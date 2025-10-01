package com.croniot.client.features.tasktypes.presentation.create_task.parameter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.data.repositories.TasksRepository
import com.croniot.client.features.tasktypes.usecases.RequestTaskStateInfoSyncUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.time.ZonedDateTime

class StatefulParameterViewModel(
    private val tasksRepository: TasksRepository,
    private val requestTaskStateInfoSyncUseCase: RequestTaskStateInfoSyncUseCase,
) : ViewModel(), KoinComponent {

    private val _statefulTaskInfoParameterSynced = MutableStateFlow(false) // TODO make Boolean? so first value can be null which means "loading", not "disconnected"
    val statefulTaskInfoParameterSynced: StateFlow<Boolean> get() = _statefulTaskInfoParameterSynced

    fun initialize(deviceUuid: String, taskTypeUid: Long) {
        val job = viewModelScope.launch {
            while (isActive) { // se cancela automáticamente al cancelar el Job
                // val latestTaskStateInfo = tasksRepository.getLatestTaskStateInfo(deviceUuid, taskTypeUid)
                val latestTaskStateInfoFromIoT = tasksRepository.getLatestTaskStateInfoEmittedByIoT(deviceUuid, taskTypeUid)
                latestTaskStateInfoFromIoT?.let {
                    val now = ZonedDateTime.now()
                    val latestTaskStateInfoDateTime = latestTaskStateInfoFromIoT.dateTime

                    val isSynced = !now.isAfter(latestTaskStateInfoDateTime.plusSeconds(5))

                    if (!isSynced) {
                        requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeUid)
                    }

                    val shouldRequestUpdate = now.isAfter(latestTaskStateInfoDateTime.plusSeconds(3))
                    if (shouldRequestUpdate) {
                        requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeUid)
                    }

                    _statefulTaskInfoParameterSynced.value = isSynced
                } ?: run {
                    _statefulTaskInfoParameterSynced.value = false

                    requestTaskStateInfoSyncUseCase(deviceUuid, taskTypeUid)
                }

                delay(1000)
            }
        }
    }
}
