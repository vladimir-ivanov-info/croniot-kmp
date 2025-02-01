package com.croniot.android.features.device.features.sensors.di

import com.croniot.android.core.data.source.repository.SensorDataRepository
import com.croniot.android.core.data.source.repository.SensorDataRepositoryImpl
import org.koin.dsl.module

object SensorsModule {

    val sensorsModule = module {
        single<SensorDataRepository> { SensorDataRepositoryImpl() }
    }
}
