package com.server.croniot.application

import com.google.gson.GsonBuilder
import croniot.models.Result
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.server.croniot.di.DI
import java.time.LocalDateTime

fun Application.configureRouting() {

    routing {
        post("/dateTime") {
            val currentDateTime = LocalDateTime.now()

            val hour = currentDateTime.hour;
            val minute = currentDateTime.minute

            val response = "$hour:$minute"

            val result = Result(true, response)
            val responseJson = GsonBuilder().setPrettyPrinting().create().toJson(result)
            call.respondText(responseJson, ContentType.Text.Plain)
        }

        post("/hour") {
            val currentDateTime = LocalDateTime.now()
            val hour = currentDateTime.hour;
            val response = "$hour"
            val result = Result(true, response)
            val responseJson = GsonBuilder().setPrettyPrinting().create().toJson(result)
            call.respondText(responseJson, ContentType.Text.Plain)
        }

        post("/minute") {
            val currentDateTime = LocalDateTime.now()
            val minute = currentDateTime.minute
            val response = "$minute"
            val result = Result(true, response)
            val responseJson = GsonBuilder().setPrettyPrinting().create().toJson(result)
            call.respondText(responseJson, ContentType.Text.Plain)
        }

        post("/second") {
            val currentDateTime = LocalDateTime.now()
            val second = currentDateTime.second
            val response = "$second"
            val result = Result(true, response)
            val responseJson = GsonBuilder().setPrettyPrinting().create().toJson(result)
            call.respondText(responseJson, ContentType.Text.Plain)
        }

        post("/api/login") {
            DI.loginController.login(call)
        }

        post("/api/iot/login") {
            DI.loginController.loginIot(call)
        }

        get("/taskConfiguration/{deviceUuid}") { //TODO rename to get_task_configurations or get_tasks_history
            DI.taskController.getTaskConfigurations(call)
        }

        post("/api/register_account") {
            DI.accountController.registerAccount(call)
        }

        post("/api/register_client"){
            DI.deviceController.registerDevice(call)
        }

        post("/api/register_sensor_type"){
            DI.sensorTypeController.registerSensorType(call)
        }

        post("/api/register_task_type"){
            DI.taskTypeController.registerTaskType(call)
        }

        post("/api/add_task"){
            DI.taskController.addTask(call)
        }

        post("/api/account_info") { //TODO probably should be  removed
            DI.accountController.processAccountInfoRequest(call)
        }
    }
}