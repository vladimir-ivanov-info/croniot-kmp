package com.server.croniot.controllers

import com.server.croniot.admin.AdminDirectoryDao
import com.server.croniot.application.AUTH_JWT_REALM
import com.server.croniot.application.JwtConfig
import com.server.croniot.application.installStatusPages
import com.server.croniot.application.requireAdmin
import com.server.croniot.testsupport.Fixtures
import croniot.messages.MessageFactory
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdminDirectoryRoutesTest {

    private val jwtConfig = JwtConfig(Fixtures.secrets())
    private val dao: AdminDirectoryDao = mockk()

    private fun Application.testModule() {
        install(ContentNegotiation) { json(MessageFactory.json) }
        install(Authentication) {
            jwt(AUTH_JWT_REALM) {
                verifier(jwtConfig.verifier())
                validate { credential ->
                    if (credential.payload.audience.contains(jwtConfig.audience)) JWTPrincipal(credential.payload) else null
                }
            }
        }
        installStatusPages()
        val controller = AdminDirectoryController(dao)
        routing {
            authenticate(AUTH_JWT_REALM) {
                get("/api/admin/devices/{uuid}") {
                    call.requireAdmin()
                    controller.getDevice(call)
                }
                get("/api/admin/accounts/{uuid}") {
                    call.requireAdmin()
                    controller.getAccount(call)
                }
            }
        }
    }

    private fun token(isAdmin: Boolean) =
        jwtConfig.issueAccessToken(accountId = 1L, email = "a@example.com", isAdmin = isAdmin).token

    @Test
    fun `WHEN the caller is not an admin THEN the pages are refused`() = testApplication {
        application { testModule() }

        val response = client.get("/api/admin/devices/dev-1") { header(HttpHeaders.Authorization, "Bearer ${token(false)}") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `WHEN the device or account does not exist THEN 404`() = testApplication {
        application { testModule() }
        every { dao.deviceDetail("ghost") } returns null
        every { dao.accountDetail("ghost") } returns null

        val device = client.get("/api/admin/devices/ghost") { header(HttpHeaders.Authorization, "Bearer ${token(true)}") }
        val account = client.get("/api/admin/accounts/ghost") { header(HttpHeaders.Authorization, "Bearer ${token(true)}") }

        assertEquals(HttpStatusCode.NotFound, device.status)
        assertEquals(HttpStatusCode.NotFound, account.status)
    }
}
