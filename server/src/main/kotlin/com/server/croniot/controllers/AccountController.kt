package com.server.croniot.controllers

import com.server.croniot.services.AccountService
import croniot.messages.MessageGetAccountInfo
import croniot.messages.MessageRegisterAccount
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import javax.inject.Inject

class AccountController @Inject constructor(
    private val accountService: AccountService,
) {
    suspend fun registerAccount(call: ApplicationCall) {
        val receivedMessage = call.receive<MessageRegisterAccount>()
        val result = accountService.registerAccount(receivedMessage)
        call.respond(result)
    }

    // TODO remove
    suspend fun processAccountInfoRequest(call: ApplicationCall) {
        val message = call.receive<MessageGetAccountInfo>()
        val result = accountService.processAccountInfoRequest(message)
        call.respond(result)
    }
}
