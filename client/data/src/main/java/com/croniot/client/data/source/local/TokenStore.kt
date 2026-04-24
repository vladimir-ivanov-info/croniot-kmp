package com.croniot.client.data.source.local

import com.croniot.client.domain.models.auth.AuthTokens

interface TokenStore {
    suspend fun saveTokens(tokens: AuthTokens)
    suspend fun getTokens(): AuthTokens?
    suspend fun clearTokens()
}