package com.server.croniot.application

import com.croniot.server.db.daos.DeviceTokenDao
import com.croniot.server.db.daos.DeviceTokenDaoImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class DeviceTokenModule {
    @Binds
    @Singleton
    abstract fun bindDeviceTokenDao(deviceTokenDaoImpl: DeviceTokenDaoImpl): DeviceTokenDao
}