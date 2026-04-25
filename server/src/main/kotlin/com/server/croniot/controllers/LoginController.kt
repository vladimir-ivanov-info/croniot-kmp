package com.server.croniot.controllers

import com.server.croniot.services.LoginService
import croniot.messages.LoginDto
import croniot.models.LogoutRequestDto
import croniot.models.RefreshTokenRequestDto
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import javax.inject.Inject

class LoginController @Inject constructor(
    private val loginService: LoginService,
) {

    suspend fun login(call: ApplicationCall) {
        val message = call.receive<LoginDto>()
        val result = loginService.login(message)
        call.respond(result)
    }

    suspend fun loginIot(call: ApplicationCall) {
        val message = call.receive<LoginDto>()
        val result = loginService.loginIot(message)
        call.respond(result)
    }

    suspend fun refreshToken(call: ApplicationCall) {
        val message = call.receive<RefreshTokenRequestDto>()
        val result = loginService.refresh(message.refreshToken)
        call.respond(result)
    }

    suspend fun logout(call: ApplicationCall) {
        val message = call.receive<LogoutRequestDto>()
        val result = loginService.logout(message.refreshToken)
        call.respond(result)
    }
}
