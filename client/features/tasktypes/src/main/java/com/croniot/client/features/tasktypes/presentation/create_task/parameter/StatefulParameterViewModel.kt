package com.croniot.client.features.tasktypes.presentation.create_task.parameter

import androidx.lifecycle.ViewModel
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.usecases.RequestTaskStateInfoSyncUseCase
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import java.time.ZonedDateTime

class StatefulParameterViewModel(
    private val tasksRepository: TasksRepository,
    private val requestTaskStateInfoSyncUseCase: RequestTaskStateInfoSyncUseCase,
) : ViewModel() {

    private val _statefulTaskInfoParameterSynced = MutableStateFlow(false)
    val statefulTaskInfoParameterSynced: StateFlow<Boolean> get() = _statefulTaskInfoParameterSynced

    fun initialize(deviceUuid: String, taskTypeUid: Long) = launchInVmScope {
        while (isActive) {
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
        }
    }
}
