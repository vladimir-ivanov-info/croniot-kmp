package com.croniot.client.features.tasktypes.di

import com.croniot.client.features.tasktypes.presentation.create_task.CreateTaskViewModel
import com.croniot.client.features.tasktypes.presentation.create_task.parameter.StatefulParameterViewModel
import com.croniot.client.features.tasktypes.usecases.RequestTaskStateInfoSyncUseCase
import com.croniot.client.features.tasktypes.usecases.SendNewTaskUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val taskTypeModule = module {

    factory { SendNewTaskUseCase(networkUtilImpl = get()) }

    viewModel {
        CreateTaskViewModel(
            localDataRepository = get(),
            tasksRepository = get(),
            sendNewTaskUseCase = get(),
            fetchTasksUseCase = get(),
        )
    }

    viewModel {
        StatefulParameterViewModel(
            tasksRepository = get(),
            requestTaskStateInfoSyncUseCase = get(),
        )
    }

    factory {
        RequestTaskStateInfoSyncUseCase(
            networkUtilImpl = get(),
        )
    }
}
