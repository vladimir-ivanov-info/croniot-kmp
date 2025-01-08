package com.croniot.android.core.di

import com.croniot.android.app.GlobalViewModel
import com.croniot.android.core.presentation.SharedPreferencesViewModel
import com.croniot.android.features.configuration.ConfigurationViewModel
import com.croniot.android.features.device.features.sensors.presentation.ViewModelSensors
import com.croniot.android.features.device.presentation.DeviceScreenViewModel
import com.croniot.android.features.login.presentation.LoginViewModel
import com.croniot.android.features.deviceslist.DevicesListViewModel
import com.croniot.android.features.registeraccount.presentation.ViewModelRegisterAccount
import com.croniot.android.features.device.features.tasks.ViewModelTasks
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object DependencyInjectionModule {

    val dependencyInjectionModule = module {
        single { ConfigurationViewModel(androidApplication()) } // Provide Application context
        single { GlobalViewModel() }
        //viewModel { ViewModelRegisterAccount() }
        viewModel { LoginViewModel() } //TODO move to LoginModule
        single { DevicesListViewModel() }
        single { ViewModelSensors() }
        single { ViewModelTasks() }

        single { DeviceScreenViewModel() }
        single { SharedPreferencesViewModel() }
    }
}