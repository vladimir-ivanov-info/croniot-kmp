package com.croniot.client.features.login.di

import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.features.login.domain.usecase.LogInUseCase
import com.croniot.client.features.login.ui.LoginViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

object LoginModule {
    val loginModule = module {

        single<LoginDataSource> {
            get<Retrofit>().create(LoginDataSource::class.java)
        }

        factory {
            LogInUseCase(
                localDataRepository = get(),
                authRepository = get(),
                sessionRepository = get(),
                accountRepository = get(),
            )
        }

        viewModel {
            LoginViewModel(
                loginUseCase = get(),
                savedStateHandle = get(),
                dataSourceBus = get(),
            )
        }
    }
}
