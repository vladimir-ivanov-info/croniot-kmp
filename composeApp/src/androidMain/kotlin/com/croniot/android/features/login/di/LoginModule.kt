package com.croniot.android.features.login.di

import org.koin.dsl.module
import retrofit2.Retrofit
import com.croniot.android.features.login.data.repository.LoginRepositoryImpl
import com.croniot.android.features.login.domain.repository.LoginRepository
import com.croniot.android.features.login.data.LoginApiService
import com.croniot.android.features.login.presentation.LoginViewModel
import com.croniot.android.features.login.usecase.LoginUseCase
import com.croniot.android.features.login.usecase.LogoutUseCase
import org.koin.androidx.viewmodel.dsl.viewModel

object LoginModule{
    val loginModule = module {

        single<LoginApiService> {
            get<Retrofit>().create(LoginApiService::class.java)
        }

        single<LoginRepository> { LoginRepositoryImpl(apiService = get()) }

        factory { LoginUseCase(repository = get()) }

        factory { LogoutUseCase() }

        viewModel { LoginViewModel(loginUseCase = get(), logoutUseCase = get(), accountRepository = get()) }
    }
}
