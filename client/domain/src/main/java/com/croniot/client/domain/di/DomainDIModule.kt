package com.croniot.client.domain.di

import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase
import com.croniot.client.domain.usecases.GetDeviceUseCase
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
import com.croniot.client.domain.usecases.ble.ActivateBleOnlyModeUseCase
import com.croniot.client.domain.usecases.ble.ConnectBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ExitBleOnlyModeUseCase
import com.croniot.client.domain.usecases.ble.ForgetBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ObserveKnownBleDevicesUseCase
import com.croniot.client.domain.usecases.ble.PairBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ScanBleDevicesUseCase
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
            appSessionRepository = get(),
        )
    }

    factory {
        LogInUseCase(
            authRepository = get(),
            localDataRepository = get(),
            sessionRepository = get(),
            accountRepository = get(),
            devicePropertiesProvider = get(),
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

    factory { GetDeviceUseCase(appSessionRepository = get(), bleDevicesRepository = get()) }

    factory { ScanBleDevicesUseCase(bleDevicesRepository = get()) }
    factory { ObserveKnownBleDevicesUseCase(bleDevicesRepository = get()) }
    factory { PairBleDeviceUseCase(bleDevicesRepository = get()) }
    factory { ConnectBleDeviceUseCase(bleDevicesRepository = get()) }
    factory { ForgetBleDeviceUseCase(bleDevicesRepository = get()) }

    factory {
        ActivateBleOnlyModeUseCase(
            stopDeviceListenersUseCase = get(),
            appSessionRepository = get(),
        )
    }

    factory {
        ExitBleOnlyModeUseCase(
            bleDevicesRepository = get(),
            appSessionRepository = get(),
        )
    }
}
