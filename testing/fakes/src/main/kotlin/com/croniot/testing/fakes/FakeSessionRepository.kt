package com.croniot.testing.fakes

import com.croniot.client.domain.models.auth.AuthSession
import com.croniot.client.domain.models.auth.AuthTokens
import com.croniot.client.domain.repositories.SessionRepository

class FakeSessionRepository : SessionRepository {

    var savedSession: AuthSession? = null
        private set

    var savedTokens: AuthTokens? = null
        private set

    var clearAllExceptDeviceUuidCalls: Int = 0
        private set

    var clearTokensCalls: Int = 0
        private set

    override suspend fun save(session: AuthSession) {
        savedSession = session
    }

    override suspend fun saveTokens(tokens: AuthTokens) {
        savedTokens = tokens
    }

    override suspend fun getTokens(): AuthTokens? = savedTokens

    override suspend fun clearTokens() {
        clearTokensCalls++
        savedTokens = null
    }

    override suspend fun clearAllExceptDeviceUuid() {
        clearAllExceptDeviceUuidCalls++
        savedTokens = null
        savedSession = null
    }
}
