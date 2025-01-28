package com.server.croniot.controllers

import com.server.croniot.services.TaskTypeService
import croniot.messages.MessageRegisterTaskType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import javax.inject.Inject

class TaskTypeController @Inject constructor(
    private val taskTypeService: TaskTypeService
) {

    suspend fun registerTaskType(call: ApplicationCall){
        val message =  call.receive<MessageRegisterTaskType>()
        val result = taskTypeService.registerTaskType(message)
        call.respond(result)
    }

}