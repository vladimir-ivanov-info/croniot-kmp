package com.croniot.client.features.taskhistory.di

import com.croniot.client.features.taskhistory.presentation.TaskHistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object TaskHistoryModule {
    val taskHistoryModule = module {
        viewModel {
            TaskHistoryViewModel(
                fetchTaskStateInfoHistoryUseCase = get(),
                fetchTaskStateInfoHistoryCountUseCase = get(),
                tasksRepository = get(),
                taskTypesRepository = get(),
            )
        }
    }
}
