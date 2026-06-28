package com.croniot.client.features.tasktypes.di

import com.croniot.client.features.tasktypes.presentation.create_task.CreateTaskViewModel
import com.croniot.client.features.tasktypes.presentation.create_task.parameter.StatefulParameterViewModel
import com.croniot.client.features.tasktypes.presentation.tasktypes.TaskTypesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object TaskTypesModule {

    val taskTypesModule = module {

        viewModel {
            CreateTaskViewModel(
                getDeviceUseCase = get(),
                sendNewTaskUseCase = get(),
                observeTaskStateInfoUseCase = get(),
                getLatestTaskStateInfoUseCase = get(),
            )
        }

        viewModel {
            StatefulParameterViewModel(
                tasksRepository = get(),
                requestTaskStateInfoSyncUseCase = get(),
            )
        }

        viewModel {
            TaskTypesViewModel(
                fetchTasksUseCase = get(),
                requestTaskStateInfoSyncUseCase = get(),
                observeTaskStateInfoUseCase = get(),
                getLatestTaskStateInfoUseCase = get(),
            )
        }
    }
}
