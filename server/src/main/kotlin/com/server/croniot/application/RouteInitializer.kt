package com.server.croniot.application

import com.server.croniot.controllers.*
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import javax.inject.Inject
import io.ktor.server.routing.*

class RouteInitializer @Inject constructor(
    private val loginController: LoginController,
    private val accountController: AccountController,
    private val deviceController: DeviceController,
    private val taskController: TaskController,
    private val sensorTypeController: SensorTypeController,
    private val taskTypeController: TaskTypeController
) {
    fun setupRoutes(application: Application) {
        application.routing {
            post("/api/login") {
                loginController.login(call)
            }

            post("/api/iot/login") {
                loginController.loginIot(call)
            }

            get("/taskConfiguration/{deviceUuid}") {
                taskController.getTaskConfigurations(call)
            }

            post("/api/register_account") {
                accountController.registerAccount(call)
            }

            post("/api/register_client") {
                deviceController.registerDevice(call)
            }

            post("/api/register_sensor_type") {
                sensorTypeController.registerSensorType(call)
            }

            post("/api/register_task_type") {
                taskTypeController.registerTaskType(call)
            }

            post("/api/add_task"){
                taskController.addTask(call)
            }
        }
    }
}