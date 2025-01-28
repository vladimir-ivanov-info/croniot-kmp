package com.server.croniot.controllers

import com.server.croniot.services.SensorTypeService
import croniot.messages.MessageRegisterSensorType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import javax.inject.Inject

class SensorTypeController @Inject constructor(
    private val sensorTypeService: SensorTypeService
) {

    suspend fun registerSensorType(call: ApplicationCall){
        val message =  call.receive<MessageRegisterSensorType>()
        val result = sensorTypeService.registerSensorType(message)
        call.respond(result)
    }


}