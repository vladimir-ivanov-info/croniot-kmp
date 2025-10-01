package com.croniot.android.core.di

import com.croniot.android.core.presentation.splash.SplashScreenViewModel
import com.croniot.android.features.configuration.ConfigurationScreenViewModel
import com.croniot.android.features.device.features.tasks.TasksViewModel
import com.croniot.android.features.device.presentation.DeviceScreenViewModel
import com.croniot.android.features.devicelist.DeviceListViewModel
import com.croniot.client.data.repositories.LocalDataRepository
import com.croniot.client.data.repositories.LocalDataRepositoryImpl
import com.croniot.client.data.repositories.TasksRepository
import com.croniot.client.data.repositories.TasksRepositoryImpl
import com.croniot.client.data.source.local.DataStoreController
import com.croniot.client.data.source.local.LocalDatasource
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.features.sensors.domain.repository.SensorDataRepositoryImpl
import com.croniot.client.features.sensors.presentation.SensorsViewModel
import com.croniot.client.features.tasktypes.presentation.create_task.CreateTaskViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object MainDIModule {

    val mainDIModule = module {

        single<LocalDatasource> { DataStoreController() }

        single<LocalDataRepository> { LocalDataRepositoryImpl(get()) }

        single { ConfigurationScreenViewModel(localDatasource = get()) }

        single<SensorDataRepository> {
            SensorDataRepositoryImpl(
                remoteSensorDataSource = get(),
                localSensorDataSource = get(),
            )
        }

        single {
            DeviceListViewModel(
                localDataRepository = get(),
                sensorDataRepository = get(),
                logOutUseCase = get(),
                savedStateHandle = get(),
                sessionRepository = get(),
            )
        }

        single {
            SensorsViewModel(
                sensorDataRepository = get(),
            )
        }

        single<TasksRepository> {
            TasksRepositoryImpl(
                tasksDataSource = get(),
            )
        }

        single {
            TasksViewModel(
                tasksRepository = get(),
                localDataRepository = get(),
                fetchTasksUseCase = get(),
                taskTypesRepository = get(),
            )
        }

        viewModel {
            DeviceScreenViewModel(
                localDataRepository = get(),
                fetchTasksUseCase = get(),
            )
        }

        single { ConfigurationScreenViewModel(get()) }

        viewModel {
            SplashScreenViewModel(
                localDataRepository = get(),
                sensorDataRepository = get(),
                tasksRepository = get(),
                taskTypeRepository = get(),
                logInUseCase = get(),
            )
        }

        viewModel {
            CreateTaskViewModel(
                localDataRepository = get(),
                tasksRepository = get(),
                sendNewTaskUseCase = get(),
                fetchTasksUseCase = get(),
            )
        }

        single {
            FetchTasksUseCase(
                tasksRepository = get(),
            )
        }
    }
}
