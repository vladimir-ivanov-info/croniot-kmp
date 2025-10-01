package com.server.croniot.controllers

import com.google.gson.Gson
import com.server.croniot.services.DeviceService
import croniot.messages.MessageRegisterDevice
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import javax.inject.Inject

class DeviceController @Inject constructor(
    private val deviceService: DeviceService,
) {

    suspend fun registerDevice(call: ApplicationCall) {
        val messageRegisterDevice = call.receive<MessageRegisterDevice>()
        val result = deviceService.registerDevice(messageRegisterDevice)
        // call.respond(result)

        val json = Gson().toJson(result)
        call.respondText(json, ContentType.Application.Json)

        // call.respond(result)
      /*  call.respondText(
            json,
            ContentType.Application.Json,
            HttpStatusCode.OK
        )*/

       /* call.response.headers.append(
            HttpHeaders.ContentLength,
            json.length.toString())
        call.respondText(json, ContentType.Application.Json)*/
    }
}
