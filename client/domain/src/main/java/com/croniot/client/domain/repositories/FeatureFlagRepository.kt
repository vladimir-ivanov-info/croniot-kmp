package com.croniot.client.domain.repositories

import Outcome
import com.croniot.client.domain.models.FeatureFlagError
import croniot.models.dto.FeatureFlagDto
import kotlinx.coroutines.flow.Flow

interface FeatureFlagRepository {
    suspend fun fetchAndCache(): Outcome<Unit, FeatureFlagError>
    fun observeFlags(): Flow<List<FeatureFlagDto>>
    fun isEnabled(name: String): Boolean
    fun startMqttListener()
}
