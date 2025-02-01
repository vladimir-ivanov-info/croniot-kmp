package com.server.croniot.application

import com.server.croniot.controllers.AccountController
import com.server.croniot.controllers.DeviceController
import com.server.croniot.controllers.LoginController
import com.server.croniot.controllers.SensorTypeController
import com.server.croniot.controllers.TaskController
import com.server.croniot.controllers.TaskTypeController
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.http.SensorsDataController
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        // Add other modules as needed
    ]
)
interface AppComponent {
    fun loginController(): LoginController
    fun accountController(): AccountController
    fun deviceController(): DeviceController
    fun taskController(): TaskController
    fun sensorTypeController(): SensorTypeController
    fun taskTypeController(): TaskTypeController
    fun sensorDataController() : SensorsDataController

    fun deviceRepository(): DeviceRepository

}
