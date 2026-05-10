package com.croniot.client.domain.repositories

import croniot.models.dto.FeatureFlagDto
import kotlinx.coroutines.flow.Flow

interface FeatureFlagRepository {
    suspend fun fetchAndCache(): Result<Unit>
    fun observeFlags(): Flow<List<FeatureFlagDto>>
    fun isEnabled(name: String): Boolean
    fun startMqttListener()
}
