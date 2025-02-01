package com.server.croniot.application

import com.server.croniot.controllers.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RouteModule {
    @Provides
    @Singleton
    fun provideLoginController(appComponent: AppComponent): LoginController {
        return appComponent.loginController()
    }

    @Provides
    @Singleton
    fun provideAccountController(appComponent: AppComponent): AccountController {
        return appComponent.accountController()
    }

    @Provides
    @Singleton
    fun provideDeviceController(appComponent: AppComponent): DeviceController {
        return appComponent.deviceController()
    }

    @Provides
    @Singleton
    fun provideTaskController(appComponent: AppComponent): TaskController {
        return appComponent.taskController()
    }

    @Provides
    @Singleton
    fun provideSensorTypeController(appComponent: AppComponent): SensorTypeController {
        return appComponent.sensorTypeController()
    }

    @Provides
    @Singleton
    fun provideTaskTypeController(appComponent: AppComponent): TaskTypeController {
        return appComponent.taskTypeController()
    }
}
