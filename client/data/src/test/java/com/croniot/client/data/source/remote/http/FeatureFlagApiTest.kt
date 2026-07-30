package com.croniot.client.data.source.remote.http

import croniot.models.dto.FeatureFlagDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeatureFlagApiTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun api(handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): FeatureFlagApi {
        val engine = MockEngine(handler)
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        return FeatureFlagApi(client)
    }

    @Test
    fun `fetchAll deserializes list of feature flags`() = runTest {
        val flags = listOf(
            FeatureFlagDto(name = "dark_mode", enabled = true),
            FeatureFlagDto(name = "beta_feature", enabled = false, description = "beta"),
        )
        var capturedUrl = ""
        val featureFlagApi = api { request ->
            capturedUrl = request.url.encodedPath
            respond(
                content = ByteReadChannel(json.encodeToString(flags)),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = featureFlagApi.fetchAll()

        assertEquals(flags, result)
        assertEquals("/api/feature_flags", capturedUrl)
    }

    @Test
    fun `fetchAll returns empty list when server has no flags`() = runTest {
        val featureFlagApi = api {
            respond(
                content = ByteReadChannel(json.encodeToString(emptyList<FeatureFlagDto>())),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        val result = featureFlagApi.fetchAll()

        assertTrue(result.isEmpty())
    }
}
