package com.croniot.android.core.di

import com.croniot.android.core.data.source.repository.AccountRepository
import com.croniot.android.core.presentation.SharedPreferencesViewModel
import com.croniot.android.features.configuration.ConfigurationViewModel
import com.croniot.android.features.device.features.sensors.presentation.ViewModelSensors
import com.croniot.android.features.device.presentation.DeviceScreenViewModel
import com.croniot.android.features.deviceslist.DevicesListViewModel
import com.croniot.android.features.device.features.tasks.ViewModelTasks
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

object DependencyInjectionModule {

    val dependencyInjectionModule = module {
        single { ConfigurationViewModel(androidApplication()) } // Provide Application context
        single { DevicesListViewModel() }
        single { ViewModelSensors() }
        single { ViewModelTasks() }

        single { DeviceScreenViewModel() }
        single { SharedPreferencesViewModel() }

        single { AccountRepository() }
    }
}