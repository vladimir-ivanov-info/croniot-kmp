package com.server.croniot.application

import com.croniot.server.db.daos.DeviceDao
import com.croniot.server.db.daos.DeviceDaoImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class DeviceModule {
    @Binds
    @Singleton
    abstract fun bindDeviceDao(deviceDaoImpl: DeviceDaoImpl): DeviceDao
}