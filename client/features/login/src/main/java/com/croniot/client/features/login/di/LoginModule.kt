package com.croniot.client.features.login.di

import com.croniot.client.features.login.presentation.LoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object LoginModule {
    val loginModule = module {

        viewModel {
            LoginViewModel(
                loginUseCase = get(),
                localDataRepository = get(),
                startDeviceListenersUseCase = get(),
                savedStateHandle = get(),
            )
        }
    }
}
