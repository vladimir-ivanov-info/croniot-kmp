package com.server.croniot.application

import com.croniot.server.db.daos.SensorTypeDao
import com.croniot.server.db.daos.SensorTypeDaoImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class SensorTypeModule {
    @Binds
    @Singleton
    abstract fun bindDeviceDao(sensorTypeDaoImpl: SensorTypeDaoImpl): SensorTypeDao
}