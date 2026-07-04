package com.croniot.android.core.di

import com.croniot.android.app.AppViewModel
import com.croniot.android.core.notifications.NotificationHelper
import com.croniot.android.core.notifications.TaskNotificationManager
import com.croniot.android.core.presentation.splash.SplashScreenViewModel
import com.croniot.android.features.configuration.ConfigurationScreenViewModel
import com.croniot.android.features.device.presentation.DeviceScreenViewModel
import com.croniot.android.features.devicelist.DeviceListViewModel
import com.croniot.client.core.util.DevicePropertiesController
import com.croniot.client.data.repositories.LocalDataRepositoryImpl
import com.croniot.client.data.repositories.TasksRepositoryImpl
import com.croniot.client.domain.DevicePropertiesProvider
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.features.sensors.presentation.SensorsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

object MainDIModule {

    val mainDIModule = module {

        single<DevicePropertiesProvider> { DevicePropertiesController }

        single { NotificationHelper(context = get()) }

        single {
            TaskNotificationManager(
                notificationHelper = get(),
                tasksRepository = get(),
                taskTypesRepository = get(),
            )
        }

        single<LocalDataRepository> {
            LocalDataRepositoryImpl(
                navigationLocalDatasource = get(),
                authLocalDatasource = get(),
                deviceLocalDatasource = get(),
                appPreferencesLocalDatasource = get(),
                serverConfigLocalDatasource = get(),
            )
        }

        viewModel { AppViewModel(localDataRepository = get()) }

        viewModel {
            ConfigurationScreenViewModel(
                serverConfigLocalDatasource = get(),
             //   hostInterceptor = get(),
                hostHolder = get(),
            )
        }

        viewModel {
            DeviceListViewModel(
                localDataRepository = get(),
                sensorDataRepository = get(),
                tasksRepository = get(),
                logOutUseCase = get(),
                startDeviceListenersUseCase = get(),
                taskNotificationManager = get(),
                appSessionRepository = get(),
                observeKnownBleDevicesUseCase = get(),
                forgetBleDeviceUseCase = get(),
            )
        }

        viewModel {
            SensorsViewModel(
                sensorDataRepository = get(),
            )
        }

        single<TasksRepository> {
            TasksRepositoryImpl(
                cloudTasksDataSource = get(),
                bleTasksDataSource = get(named("ble")),
                transportRouter = get(),
                localTaskHistoryDataSource = get(),
            )
        }

        viewModel {
            DeviceScreenViewModel(
                localDataRepository = get(),
                fetchTasksUseCase = get(),
                startDeviceListenersUseCase = get(),
                getDeviceUseCase = get(),
                observeBleRssiUseCase = get(),
            )
        }

        viewModel {
            SplashScreenViewModel(
                localDataRepository = get(),
                sessionRepository = get(),
                logOutUseCase = get(),
                startDeviceListenersUseCase = get(),
                appSessionRepository = get(),
            )
        }

        factory {
            FetchTasksUseCase(
                tasksRepository = get(),
            )
        }
    }
}
