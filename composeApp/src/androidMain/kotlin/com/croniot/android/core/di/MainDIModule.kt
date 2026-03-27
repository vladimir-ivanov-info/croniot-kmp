package com.croniot.android.core.di

import com.croniot.android.app.AppViewModel
import com.croniot.android.core.presentation.splash.SplashScreenViewModel
import com.croniot.android.features.configuration.ConfigurationScreenViewModel
//import com.croniot.android.features.device.features.tasks.TasksViewModel
import com.croniot.android.features.device.presentation.DeviceScreenViewModel
import com.croniot.android.features.devicelist.DeviceListViewModel
import com.croniot.client.data.repositories.LocalDataRepositoryImpl
import com.croniot.client.data.repositories.TasksRepositoryImpl
import com.croniot.client.data.source.local.DataStoreController
import com.croniot.client.data.source.local.LocalDatasource
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.android.core.notifications.NotificationHelper
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.features.sensors.presentation.SensorsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

object MainDIModule {

    val mainDIModule = module {

        single { NotificationHelper(context = get()) }

        single<LocalDatasource> { DataStoreController(context = get()) }

        single<LocalDataRepository> { LocalDataRepositoryImpl(get()) }

        viewModel { AppViewModel(localDataRepository = get()) }

        viewModel {
            ConfigurationScreenViewModel(
                localDatasource = get(),
                hostInterceptor = get(),
            )
        }

        viewModel {
            DeviceListViewModel(
                localDataRepository = get(),
                sensorDataRepository = get(),
                logOutUseCase = get(),
                savedStateHandle = get(),
            )
        }

        viewModel {
            SensorsViewModel(
                sensorDataRepository = get(),
            )
        }

        single<TasksRepository> {
            TasksRepositoryImpl(
                tasksDataSource = get(),
            )
        }

//        viewModel {
//            TasksViewModel(
//                tasksRepository = get(),
//                localDataRepository = get(),
//                fetchTasksUseCase = get(),
//                observeNewTasksUseCase = get(),
//                observeTaskStateInfoUseCase = get(),
//                taskTypesRepository = get(),
//            )
//        }

        viewModel {
            DeviceScreenViewModel(
                localDataRepository = get(),
                fetchTasksUseCase = get(),
            )
        }

        viewModel {
            SplashScreenViewModel(
                localDataRepository = get(),
                logInUseCase = get(),
                logOutUseCase = get(),
                startDeviceListenersUseCase = get(),
            )
        }

        factory {
            FetchTasksUseCase(
                tasksRepository = get(),
            )
        }
    }
}
