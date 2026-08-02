package com.croniot.testing.fakes

import Outcome
import com.croniot.client.domain.models.FeatureFlagError
import com.croniot.client.domain.repositories.FeatureFlagRepository
import croniot.models.dto.FeatureFlagDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeFeatureFlagRepository(
    private var fetchAndCacheOutcome: Outcome<Unit, FeatureFlagError> = Outcome.Ok(Unit),
    private var flagsFlow: Flow<List<FeatureFlagDto>> = emptyFlow(),
    private var enabledFlags: Set<String> = emptySet(),
) : FeatureFlagRepository {

    var fetchAndCacheCalls: Int = 0
        private set

    var startMqttListenerCalls: Int = 0
        private set

    override suspend fun fetchAndCache(): Outcome<Unit, FeatureFlagError> {
        fetchAndCacheCalls++
        return fetchAndCacheOutcome
    }

    override fun observeFlags(): Flow<List<FeatureFlagDto>> = flagsFlow

    override fun isEnabled(name: String): Boolean = name in enabledFlags

    override fun startMqttListener() {
        startMqttListenerCalls++
    }
}
