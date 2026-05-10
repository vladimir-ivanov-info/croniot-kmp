package com.croniot.client.data.source.remote.http

import com.croniot.client.core.config.Constants
import croniot.models.dto.FeatureFlagDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class FeatureFlagApi(private val http: HttpClient) {
    suspend fun fetchAll(): List<FeatureFlagDto> =
        http.get(Constants.ENDPOINT_FEATURE_FLAGS).body()
}
