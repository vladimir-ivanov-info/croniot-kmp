package com.server.croniot.controllers

import com.server.croniot.services.SensorTypeService
import croniot.messages.MessageRegisterSensorType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SensorTypeController @Inject constructor(
    private val sensorTypeService: SensorTypeService,
) {
   // private val json = Json { ignoreUnknownKeys = true }

    suspend fun registerSensorType(call: ApplicationCall) {

        /*val rawBody = call.receiveText()
        println("RAW BODY: $rawBody")
        call.respond("He recibido: $rawBody")
*/
        val message = call.receive<MessageRegisterSensorType>()
        val result = sensorTypeService.registerSensorType(message)
        call.respond(result)

       /* val raw = call.receiveText()
        call.application.environment.log.info("RAW BODY: $raw")

        val message = json.decodeFromString<MessageRegisterSensorType>(raw)
        val result = sensorTypeService.registerSensorType(message)
        call.respond(result)*/

    }
}
