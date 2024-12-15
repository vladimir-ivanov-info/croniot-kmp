package com.croniot.android.di

import com.croniot.android.GlobalViewModel
import com.croniot.android.SharedPreferencesViewModel
import com.croniot.android.presentation.configuration.ConfigurationViewModel
import com.croniot.android.presentation.device.sensors.ViewModelSensors
import com.croniot.android.presentation.device.DeviceScreenViewModel
import com.croniot.android.presentation.login.LoginViewModel
import com.croniot.android.presentation.devices.DevicesViewModel
import com.croniot.android.presentation.registerAccount.ViewModelRegisterAccount
import com.croniot.android.presentation.device.tasks.ViewModelTasks
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object DependencyInjectionModule {

    val dependencyInjectionModule = module {
        single { ConfigurationViewModel(androidApplication()) } // Provide Application context
        single { GlobalViewModel() }
        viewModel { ViewModelRegisterAccount() }
        viewModel { LoginViewModel() }
        single { DevicesViewModel() }
        single { ViewModelSensors() }
        single { ViewModelTasks() }

        single { DeviceScreenViewModel() }
        single { SharedPreferencesViewModel() }
    }
}