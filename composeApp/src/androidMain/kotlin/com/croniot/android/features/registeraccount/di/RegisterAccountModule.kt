package com.croniot.android.features.registeraccount.di

import com.croniot.android.features.registeraccount.domain.controller.RegisterAccountController
import com.croniot.android.features.registeraccount.domain.usecase.RegisterAccountUseCase
import com.croniot.android.features.registeraccount.presentation.ViewModelRegisterAccount
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object RegisterAccountModule {

    val registerAccountModule = module {
        factory { RegisterAccountUseCase() }
        single { RegisterAccountController(get()) }
        viewModel { ViewModelRegisterAccount(get()) }
    }
}
