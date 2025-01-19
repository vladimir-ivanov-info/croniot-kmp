package com.croniot.android.features.device.features.sensors.di

import com.croniot.android.core.data.source.repository.MqttSensorDataRepository
import org.koin.dsl.module

object SensorsModule {

    val sensorsModule = module {
        single { MqttSensorDataRepository() }
    }

}