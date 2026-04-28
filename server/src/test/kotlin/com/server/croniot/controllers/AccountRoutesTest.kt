package com.server.croniot.controllers

import com.server.croniot.application.DomainException
import com.server.croniot.application.installStatusPages
import com.server.croniot.services.AccountService
import croniot.messages.MessageFactory
import croniot.messages.MessageRegisterAccount
import croniot.models.Result as DomainResult
import croniot.models.errors.DomainError
import croniot.models.errors.ErrorResponse
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountRoutesTest {

    private val registerMessage = MessageRegisterAccount(
        accountUuid = "acc-uuid",
        nickname = "nick",
        email = "user@example.com",
        password = "secret",
    )

    private fun Application.testModule(accountService: AccountService) {
        install(ContentNegotiation) { json(MessageFactory.json) }
        installStatusPages()

        val accountController = AccountController(accountService)
        routing {
            post("/api/register_account") { accountController.registerAccount(call) }
        }
    }

    @Test
    fun `POST register_account returns 200 with success when service accepts`() = testApplication {
        val accountService = mockk<AccountService>()
        every { accountService.registerAccount(any()) } returns DomainResult(success = true, message = "")
        application { testModule(accountService) }

        val response = client.post("/api/register_account") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(registerMessage))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = MessageFactory.fromJson<DomainResult>(response.bodyAsText())
        assertTrue(body.success)
    }

    @Test
    fun `POST register_account returns 409 CONFLICT when email is taken`() = testApplication {
        val accountService = mockk<AccountService>()
        every { accountService.registerAccount(any()) } throws DomainException(
            DomainError.Conflict("This email is already used."),
        )
        application { testModule(accountService) }

        val response = client.post("/api/register_account") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MessageFactory.toJson(registerMessage))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val error = MessageFactory.fromJson<ErrorResponse>(response.bodyAsText())
        assertEquals(DomainError.Conflict.CODE, error.code)
        assertEquals("This email is already used.", error.message)
    }
}
