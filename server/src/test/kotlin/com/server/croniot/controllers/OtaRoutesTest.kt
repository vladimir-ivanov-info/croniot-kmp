package com.server.croniot.controllers

import com.server.croniot.application.AUTH_JWT_REALM
import com.server.croniot.application.AdminOnly
import com.server.croniot.application.JwtConfig
import com.server.croniot.application.installStatusPages
import com.server.croniot.data.db.daos.FirmwareRelease
import com.server.croniot.ota.FirmwareReleaseDto
import com.server.croniot.ota.OtaManifestDto
import com.server.croniot.ota.OtaRequestPublisher
import com.server.croniot.services.OtaService
import com.server.croniot.testsupport.Fixtures
import croniot.messages.MessageFactory
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class OtaRoutesTest {

    private val jwtConfig = JwtConfig(Fixtures.secrets())
    private val service: OtaService = mockk()
    private val published = mutableListOf<Pair<String, String>>()
    private val publisher = object : OtaRequestPublisher {
        override suspend fun publish(deviceUuid: String, manifestJson: String) {
            published += deviceUuid to manifestJson
        }
    }

    private val image = ByteArray(100) { it.toByte() }

    // Same wiring as RouteInitializer.
    private fun Application.testModule() {
        install(ContentNegotiation) { json(MessageFactory.json) }
        install(Authentication) {
            jwt(AUTH_JWT_REALM) {
                verifier(jwtConfig.verifier())
                validate { credential ->
                    if (credential.payload.audience.contains(
                            jwtConfig.audience
                        )
                    ) JWTPrincipal(credential.payload) else null
                }
            }
        }
        installStatusPages()
        val controller = OtaController(service, publisher)
        routing {
            get("/api/iot/firmware/{id}") { controller.downloadFirmware(call) }
            authenticate(AUTH_JWT_REALM) {
                route("/api/admin/firmware") {
                    install(AdminOnly)
                    post { controller.uploadFirmware(call) }
                    get { controller.listFirmware(call) }
                }
                route("/api/admin/devices/{uuid}/ota") {
                    install(AdminOnly)
                    put { controller.requestUpdate(call) }
                    get { controller.getUpdate(call) }
                    delete { controller.cancelUpdate(call) }
                }
            }
        }
    }

    private fun token(isAdmin: Boolean) =
        jwtConfig.issueAccessToken(accountId = 1L, email = "a@example.com", isAdmin = isAdmin).token

    private val sha = "a".repeat(64)

    private fun manifest() = OtaManifestDto("req-1", "1.2.0", "/api/iot/firmware/12", 100, sha)

    @Test
    fun `WHEN the caller is not an admin THEN firmware cannot be uploaded or requested`() = testApplication {
        application { testModule() }

        val upload = client.post("/api/admin/firmware") {
            header(HttpHeaders.Authorization, "Bearer ${token(false)}")
            setBody(image)
        }
        val request = client.put("/api/admin/devices/dev/ota") {
            header(HttpHeaders.Authorization, "Bearer ${token(false)}")
            contentType(ContentType.Application.Json)
            setBody("""{"releaseId":12}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, upload.status)
        assertEquals(HttpStatusCode.Unauthorized, request.status)
        assertTrue(published.isEmpty())
    }

    @Test
    fun `WHEN an admin uploads an image THEN the stored release comes back`() = testApplication {
        application { testModule() }
        every { service.upload(image) } returns FirmwareRelease(12, "p", "1.2.0", 100, "a".repeat(64), OffsetDateTime.now())

        val response = client.post("/api/admin/firmware") {
            header(HttpHeaders.Authorization, "Bearer ${token(true)}")
            contentType(ContentType.Application.OctetStream)
            setBody(image)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val release = MessageFactory.json.decodeFromString(FirmwareReleaseDto.serializer(), response.bodyAsText())
        assertEquals("1.2.0", release.version)
        assertEquals(12L, release.id)
    }

    @Test
    fun `WHEN an admin requests an update THEN the manifest is published retained for that device`() = testApplication {
        application { testModule() }
        every { service.requestUpdate("dev", 12L) } returns manifest()

        val response = client.put("/api/admin/devices/dev/ota") {
            header(HttpHeaders.Authorization, "Bearer ${token(true)}")
            contentType(ContentType.Application.Json)
            setBody("""{"releaseId":12}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, published.size)
        assertEquals("dev", published[0].first)
        assertEquals(
            """{"requestId":"req-1","version":"1.2.0","path":"/api/iot/firmware/12","size":100,"sha256":"$sha"}""",
            published[0].second,
        )
    }

    @Test
    fun `WHEN an admin cancels THEN the retained request is cleared with an empty payload`() = testApplication {
        application { testModule() }
        every { service.cancel("dev") } returns false

        val response = client.delete("/api/admin/devices/dev/ota") {
            header(HttpHeaders.Authorization, "Bearer ${token(true)}")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(listOf("dev" to ""), published)
    }

    @Test
    fun `WHEN the device downloads its release THEN it gets the whole image`() = testApplication {
        application { testModule() }
        every { service.imageForDevice("dev", "tok", 12L) } returns image

        val response = client.get("/api/iot/firmware/12") {
            header("X-Device-Uuid", "dev")
            header("X-Device-Token", "tok")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("bytes", response.headers[HttpHeaders.AcceptRanges])
        assertArrayEquals(image, response.readRawBytes())
    }

    @Test
    fun `WHEN the device resumes a download THEN it gets the rest of the image`() = testApplication {
        application { testModule() }
        every { service.imageForDevice("dev", "tok", 12L) } returns image

        val response = client.get("/api/iot/firmware/12") {
            header("X-Device-Uuid", "dev")
            header("X-Device-Token", "tok")
            header(HttpHeaders.Range, "bytes=60-")
        }

        assertEquals(HttpStatusCode.PartialContent, response.status)
        assertEquals("bytes 60-99/100", response.headers[HttpHeaders.ContentRange])
        assertArrayEquals(image.copyOfRange(60, 100), response.readRawBytes())
    }

    @Test
    fun `WHEN the device credentials are wrong THEN the download is refused`() = testApplication {
        application { testModule() }
        every { service.imageForDevice("dev", "bad", 12L) } throws
            com.server.croniot.application.DomainException(
                croniot.models.errors.DomainError.Unauthorized("Invalid device credentials")
            )

        val response = client.get("/api/iot/firmware/12") {
            header("X-Device-Uuid", "dev")
            header("X-Device-Token", "bad")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        verify { service.imageForDevice("dev", "bad", 12L) }
    }
}
