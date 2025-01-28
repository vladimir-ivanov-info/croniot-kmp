package com.server.croniot.controllers

import com.server.croniot.services.LoginService
import croniot.messages.MessageLoginRequest
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import javax.inject.Inject

class LoginController @Inject constructor(
    private val loginService: LoginService
) {

    suspend fun login(call: ApplicationCall){
        val message =  call.receive<MessageLoginRequest>()
        val result = loginService.login(message)
        call.respond(result)
    }

    suspend fun loginIot(call: ApplicationCall){
        val message =  call.receive<MessageLoginRequest>()
        val result = loginService.loginIot(message)
        call.respond(result)
    }

}