package com.croniot.client.data.repositories

import com.croniot.client.domain.models.auth.AuthSession
import com.croniot.client.domain.models.auth.AuthTokens
import com.croniot.client.data.source.local.AppPreferencesLocalDatasource
import com.croniot.client.data.source.local.AuthLocalDatasource
import com.croniot.client.data.source.local.TokenStore
import com.croniot.client.domain.repositories.SessionRepository

class SessionRepositoryImpl(
    private val authLocalDatasource: AuthLocalDatasource,
    private val appPreferencesLocalDatasource: AppPreferencesLocalDatasource,
    private val tokenStore: TokenStore,
) : SessionRepository {

    override suspend fun save(session: AuthSession) {
        authLocalDatasource.saveEmail(session.email)
    }

    override suspend fun saveTokens(tokens: AuthTokens) {
        tokenStore.saveTokens(tokens)
    }

    override suspend fun getTokens(): AuthTokens? {
        return tokenStore.getTokens()
    }

    override suspend fun clearTokens() {
        tokenStore.clearTokens()
    }

    override suspend fun clearAllExceptDeviceUuid() {
        tokenStore.clearTokens()
        appPreferencesLocalDatasource.clearAllCacheExceptDeviceUuid()
    }
}