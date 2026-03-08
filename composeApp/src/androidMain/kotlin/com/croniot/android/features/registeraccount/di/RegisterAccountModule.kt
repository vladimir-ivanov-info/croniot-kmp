package com.croniot.android.features.registeraccount.di

import com.croniot.android.features.registeraccount.data.RegisterAccountRepositoryImpl
import com.croniot.client.domain.repositories.RegisterAccountRepository
import com.croniot.client.domain.usecases.RegisterAccountUseCase
import com.croniot.android.features.registeraccount.presentation.ViewModelRegisterAccount
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object RegisterAccountModule {

    val registerAccountModule = module {
        single<RegisterAccountRepository> { RegisterAccountRepositoryImpl(networkUtil = get()) }
        factory { RegisterAccountUseCase(repository = get()) }
        viewModel {
            ViewModelRegisterAccount(
                registerAccountUseCase = get(),
                savedStateHandle = get(),
            )
        }
    }
}
