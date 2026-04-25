package com.croniot.client.domain.repositories

import com.croniot.client.domain.models.auth.AuthSession
import com.croniot.client.domain.models.auth.AuthTokens

interface SessionRepository {

    suspend fun save(session: AuthSession)
    suspend fun saveTokens(tokens: AuthTokens)
    suspend fun getTokens(): AuthTokens?
    suspend fun clearTokens()
    suspend fun clearAllExceptDeviceUuid()
}
