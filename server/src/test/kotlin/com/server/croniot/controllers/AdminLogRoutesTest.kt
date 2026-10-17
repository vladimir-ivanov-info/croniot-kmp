package com.server.croniot.controllers

import com.server.croniot.application.AUTH_JWT_REALM
import com.server.croniot.application.JwtConfig
import com.server.croniot.application.installStatusPages
import com.server.croniot.application.requireAdmin
import com.server.croniot.livelog.LiveLogFilter
import com.server.croniot.livelog.LogEntryDto
import com.server.croniot.livelog.LogHistoryDao
import com.server.croniot.livelog.LogHistoryQuery
import com.server.croniot.livelog.LogKind
import com.server.croniot.livelog.LogLevels
import com.server.croniot.livelog.LogPlatform
import com.server.croniot.testsupport.Fixtures
import croniot.messages.MessageFactory
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
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
import io.mockk.slot
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdminLogRoutesTest {

    private val jwtConfig = JwtConfig(Fixtures.secrets())
    private val dao: LogHistoryDao = mockk()

    private fun Application.testModule() {
        install(ContentNegotiation) { json(MessageFactory.json) }
        install(Authentication) {
            jwt(AUTH_JWT_REALM) {
                verifier(jwtConfig.verifier())
                validate { credential ->
                    if (credential.payload.audience.contains(
                            jwtConfig.audience
                        )
                    ) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }
        installStatusPages()
        val controller = AdminLogController(dao)
        routing {
            authenticate(AUTH_JWT_REALM) {
                get("/api/admin/logs") {
                    call.requireAdmin()
                    controller.getLogs(call)
                }
            }
        }
    }

    private fun token(isAdmin: Boolean) =
        jwtConfig.issueAccessToken(accountId = 1L, email = "a@example.com", isAdmin = isAdmin).token

    @Test
    fun `WHEN the caller is not an admin THEN history is refused`() = testApplication {
        application { testModule() }

        val response = client.get("/api/admin/logs") { header(HttpHeaders.Authorization, "Bearer ${token(false)}") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `WHEN an admin asks THEN the query is parsed and entries come back as JSON`() = testApplication {
        application { testModule() }
        val entry = LogEntryDto(
            id = "log-1",
            platform = LogPlatform.IOT,
            kind = LogKind.LOG,
            level = LogLevels.WARN,
            tag = "Uplink",
            message = "m",
            receivedAtMs = 5,
        )
        val query = slot<LogHistoryQuery>()
        every { dao.find(capture(query)) } returns listOf(entry)

        val response = client.get("/api/admin/logs?platform=IOT&level=warn&q=fail&before=10&beforeId=log-9&limit=50") {
            header(HttpHeaders.Authorization, "Bearer ${token(true)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            listOf(entry),
            Json.decodeFromString(ListSerializer(LogEntryDto.serializer()), response.bodyAsText())
        )
        assertEquals(
            LogHistoryQuery(
                filter = LiveLogFilter(platforms = setOf(LogPlatform.IOT), levels = setOf(LogLevels.WARN)),
                text = "fail",
                beforeMs = 10,
                beforeId = "log-9",
                limit = 50,
            ),
            query.captured,
        )
    }
}
