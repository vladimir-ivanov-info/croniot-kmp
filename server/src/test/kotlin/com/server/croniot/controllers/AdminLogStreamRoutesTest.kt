package com.server.croniot.controllers

import com.server.croniot.application.AUTH_JWT_REALM
import com.server.croniot.application.AdminOnly
import com.server.croniot.application.JwtConfig
import com.server.croniot.application.installStatusPages
import com.server.croniot.livelog.LiveLogBus
import com.server.croniot.livelog.LogEntryDto
import com.server.croniot.livelog.LogKind
import com.server.croniot.livelog.LogLevels
import com.server.croniot.livelog.LogPlatform
import com.server.croniot.testsupport.Fixtures
import croniot.messages.MessageFactory
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
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
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdminLogStreamRoutesTest {

    private val jwtConfig = JwtConfig(Fixtures.secrets())
    private val bus = LiveLogBus()

    private fun Application.testModule() {
        install(ContentNegotiation) { json(MessageFactory.json) }
        install(Authentication) {
            jwt(AUTH_JWT_REALM) {
                verifier(jwtConfig.verifier())
                validate { credential ->
                    val subject = credential.payload.subject
                    val hasAudience = credential.payload.audience.contains(jwtConfig.audience)
                    if (!subject.isNullOrBlank() && hasAudience) JWTPrincipal(credential.payload) else null
                }
            }
        }
        installStatusPages()
        install(io.ktor.server.sse.SSE)

        val controller = AdminLogStreamController(bus)
        routing {
            authenticate(AUTH_JWT_REALM) {
                route("/api/admin/logs/stream") {
                    install(AdminOnly)
                    sse { controller.stream(this) }
                }
            }
        }
    }

    private fun adminToken() =
        jwtConfig.issueAccessToken(accountId = 1L, email = "admin@example.com", isAdmin = true).token

    private fun userToken() =
        jwtConfig.issueAccessToken(accountId = 2L, email = "user@example.com", isAdmin = false).token

    private fun entry(id: String, platform: LogPlatform, level: Int) = LogEntryDto(
        id = id, platform = platform, kind = LogKind.LOG, level = level, tag = "T", message = "m $id", receivedAtMs = 1,
    )

    @Test
    fun `WHEN there is no token THEN the stream is refused`() = testApplication {
        application { testModule() }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/admin/logs/stream").status)
    }

    @Test
    fun `WHEN the user is not an admin THEN the stream is refused`() = testApplication {
        application { testModule() }

        val response = client.get("/api/admin/logs/stream") { header(HttpHeaders.Authorization, "Bearer ${userToken()}") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `WHEN an admin subscribes with filters THEN only matching entries arrive, as JSON with their id`() = testApplication {
        application { testModule() }
        val sseClient = createClient { install(SSE) }

        val received = coroutineScope {
            val events = async(start = CoroutineStart.UNDISPATCHED) {
                val list = mutableListOf<Pair<String?, String?>>()
                sseClient.sse(
                    urlString = "/api/admin/logs/stream?platform=IOT&level=error,warn",
                    request = { header(HttpHeaders.Authorization, "Bearer ${adminToken()}") },
                ) {
                    incoming.filter { it.event == "log" }.take(2).toList().forEach { list.add(it.id to it.data) }
                }
                list
            }
            withTimeout(10_000) { while (bus.subscriberCount < 1) yield() }

            bus.publish(entry("log-1", LogPlatform.IOT, LogLevels.ERROR))
            bus.publish(entry("srv-1", LogPlatform.SERVER, LogLevels.ERROR)) // wrong platform
            bus.publish(entry("log-2", LogPlatform.IOT, LogLevels.INFO)) // wrong level
            bus.publish(entry("log-3", LogPlatform.IOT, LogLevels.WARN))

            withTimeout(10_000) { events.await() }
        }

        assertEquals(listOf("log-1", "log-3"), received.map { it.first })
        val decoded = Json.decodeFromString(LogEntryDto.serializer(), received.first().second!!)
        assertEquals(entry("log-1", LogPlatform.IOT, LogLevels.ERROR), decoded)
    }
}
