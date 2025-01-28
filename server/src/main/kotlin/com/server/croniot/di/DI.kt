package com.server.croniot.di

import com.server.croniot.application.AppComponent
import com.server.croniot.application.DaggerAppComponent
import com.server.croniot.controllers.AccountController
import com.server.croniot.controllers.DeviceController
import com.server.croniot.controllers.LoginController
import com.server.croniot.controllers.SensorTypeController
import com.server.croniot.controllers.TaskController
import com.server.croniot.controllers.TaskTypeController
import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository

object DI {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.create()
    }

    val accountController: AccountController by lazy {
        appComponent.accountController()
    }

    val deviceController: DeviceController by lazy {
        appComponent.deviceController()
    }

    val loginController: LoginController by lazy {
        appComponent.loginController()
    }

    val sensorTypeController: SensorTypeController by lazy {
        appComponent.sensorTypeController()
    }

    val taskTypeController: TaskTypeController by lazy {
        appComponent.taskTypeController()
    }

    val taskController: TaskController by lazy {
        appComponent.taskController()
    }



    val accountRepository: AccountRepository by lazy {
        appComponent.accountRepository()
    }

    val deviceRepository: DeviceRepository by lazy {
        appComponent.deviceRepository()
    }

}