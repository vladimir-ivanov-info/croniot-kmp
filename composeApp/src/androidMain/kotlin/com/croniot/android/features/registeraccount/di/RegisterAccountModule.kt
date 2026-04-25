package com.croniot.android.features.registeraccount.di

import com.croniot.client.data.repositories.RegisterAccountRepositoryImpl
import com.croniot.android.features.registeraccount.presentation.ViewModelRegisterAccount
import com.croniot.client.domain.repositories.RegisterAccountRepository
import com.croniot.client.domain.usecases.RegisterAccountUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object RegisterAccountModule {

    val registerAccountModule = module {
        single<RegisterAccountRepository> { RegisterAccountRepositoryImpl(registerApi = get()) }
        factory { RegisterAccountUseCase(repository = get()) }
        viewModel {
            ViewModelRegisterAccount(
                registerAccountUseCase = get(),
                savedStateHandle = get(),
            )
        }
    }
}
