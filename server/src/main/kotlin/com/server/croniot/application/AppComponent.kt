package com.server.croniot.application

import com.server.croniot.controllers.AccountController
import com.server.croniot.controllers.DeviceController
import com.server.croniot.controllers.LoginController
import com.server.croniot.controllers.SensorTypeController
import com.server.croniot.controllers.TaskController
import com.server.croniot.controllers.TaskTypeController
import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.http.SensorsDataController
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(accountController: AccountController)

    fun accountController(): AccountController

    fun accountRepository(): AccountRepository

    fun deviceController(): DeviceController

    fun deviceRepository(): DeviceRepository



    fun loginController(): LoginController

    fun sensorTypeController() : SensorTypeController

    fun taskTypeController() : TaskTypeController

    fun taskController() : TaskController

    fun sensorDataController() : SensorsDataController


}