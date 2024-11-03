package com.croniot.android.di

import com.croniot.android.SharedPreferencesViewModel
import com.croniot.android.presentation.device.sensors.ViewModelSensors
import com.croniot.android.presentation.device.DeviceScreenViewModel
import com.croniot.android.presentation.login.LoginViewModel
import com.croniot.android.presentation.devices.DevicesViewModel
import com.croniot.android.ui.task.ViewModelTasks
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object DependencyInjectionModule {

    val dependencyInjectionModule = module {
        viewModel { LoginViewModel() }
        single { DevicesViewModel() }
        single { ViewModelSensors() }
        single { ViewModelTasks() }
        single { DeviceScreenViewModel() }
        single { SharedPreferencesViewModel() }
    }

}