package com.croniot.client.domain.di

import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryCountUseCase
import com.croniot.client.domain.usecases.GetLatestTaskStateInfoUseCase
import com.croniot.client.domain.usecases.LogInUseCase
import com.croniot.client.domain.usecases.LogoutUseCase
import com.croniot.client.domain.usecases.ObserveNewTasksUseCase
import com.croniot.client.domain.usecases.ObserveTaskStateInfoUseCase
import com.croniot.client.domain.usecases.RequestTaskStateInfoSyncUseCase
import com.croniot.client.domain.usecases.RequestTaskStateInfoSyncUseCaseImpl
import com.croniot.client.domain.usecases.SendNewTaskUseCase
import com.croniot.client.domain.usecases.SendNewTaskUseCaseImpl
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.client.domain.usecases.StopDeviceListenersUseCase
import org.koin.dsl.module

val domainDiModule = module {

    factory {
        StopDeviceListenersUseCase(
            sensorDataRepository = get(),
            tasksRepository = get(),
        )
    }

    factory {
        LogoutUseCase(
            sessionRepository = get(),
            stopDeviceListenersUseCase = get(),
        )
    }

    factory {
        LogInUseCase(
            authRepository = get(),
            localDataRepository = get(),
            sessionRepository = get(),
            accountRepository = get(),
        )
    }

    factory { FetchTaskStateInfoHistoryUseCase(tasksRepository = get()) }
    factory { FetchTaskStateInfoHistoryCountUseCase(tasksRepository = get()) }
    factory { ObserveNewTasksUseCase(tasksRepository = get()) }
    factory { ObserveTaskStateInfoUseCase(tasksRepository = get()) }
    factory { GetLatestTaskStateInfoUseCase(tasksRepository = get()) }
    factory<SendNewTaskUseCase> { SendNewTaskUseCaseImpl(tasksRepository = get()) }
    factory<RequestTaskStateInfoSyncUseCase> { RequestTaskStateInfoSyncUseCaseImpl(tasksRepository = get()) }

    factory {
        StartDeviceListenersUseCase(
            sensorDataRepository = get(),
            tasksRepository = get(),
            taskTypesRepository = get(),
        )
    }
}
