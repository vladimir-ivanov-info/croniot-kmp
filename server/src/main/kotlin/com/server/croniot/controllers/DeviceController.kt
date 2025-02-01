package com.server.croniot.controllers

import com.server.croniot.services.DeviceService
import croniot.messages.MessageRegisterDevice
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import javax.inject.Inject

class DeviceController @Inject constructor(
    private val deviceService: DeviceService
){

    suspend fun registerDevice(call: ApplicationCall){
        val messageRegisterDevice = call.receive<MessageRegisterDevice>()
        val result = deviceService.registerDevice(messageRegisterDevice)
        call.respond(result)
    }

}