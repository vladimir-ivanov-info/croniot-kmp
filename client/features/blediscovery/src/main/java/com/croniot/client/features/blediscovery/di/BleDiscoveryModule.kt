package com.croniot.client.features.blediscovery.di

import com.croniot.client.features.blediscovery.presentation.BleDiscoveryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object BleDiscoveryModule {
    val bleDiscoveryModule = module {

        viewModel {
            BleDiscoveryViewModel(
                scanBleDevicesUseCase = get(),
                observeKnownBleDevicesUseCase = get(),
                pairBleDeviceUseCase = get(),
                connectBleDeviceUseCase = get(),
                forgetBleDeviceUseCase = get(),
                activateBleOnlyModeUseCase = get(),
                permissionsHelper = get(),
            )
        }
    }
}
