package com.server.croniot.application

import com.google.gson.GsonBuilder
import com.server.croniot.controllers.*
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.time.LocalDateTime
import javax.inject.Inject

class RouteInitializer @Inject constructor(
    private val loginController: LoginController,
    private val accountController: AccountController,
    private val deviceController: DeviceController,
    private val taskController: TaskController,
    private val sensorTypeController: SensorTypeController,
    private val taskTypeController: TaskTypeController,
) {
    fun setupRoutes(application: Application) {
        application.routing {

            post("/dateTime") {
                val currentDateTime = LocalDateTime.now()

                val hour = currentDateTime.hour;
                val minute = currentDateTime.minute

                val response = "$hour:$minute"

                val result = croniot.models.Result(true, response)
                val responseJson = GsonBuilder().setPrettyPrinting().create().toJson(result)
                call.respondText(responseJson, ContentType.Text.Plain)
            }

            post("/hour") {
                val currentDateTime = LocalDateTime.now()
                val hour = currentDateTime.hour;
                val response = "$hour"
                val result = croniot.models.Result(true, response)
                val responseJson = GsonBuilder().setPrettyPrinting().create().toJson(result)
                call.respondText(responseJson, ContentType.Text.Plain)
            }

            post("/minute") {
                val currentDateTime = LocalDateTime.now()
                val minute = currentDateTime.minute
                val response = "$minute"
                val result = croniot.models.Result(true, response)
                val responseJson = GsonBuilder().setPrettyPrinting().create().toJson(result)
                call.respondText(responseJson, ContentType.Text.Plain)
            }

            post("/second") {
                val currentDateTime = LocalDateTime.now()
                val second = currentDateTime.second
                val response = "$second"
                val result = croniot.models.Result(true, response)
                val responseJson = GsonBuilder().setPrettyPrinting().create().toJson(result)
                call.respondText(responseJson, ContentType.Text.Plain)
            }

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

            post("/api/add_task") {
                taskController.addTask(call)
            }
        }
    }
}
