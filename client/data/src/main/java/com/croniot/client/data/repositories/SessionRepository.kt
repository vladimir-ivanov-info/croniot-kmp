package com.croniot.client.data.repositories

import com.croniot.client.core.models.auth.AuthSession

interface SessionRepository {

    suspend fun save(session: AuthSession)
    suspend fun clearAllExceptDeviceUuid()

    // suspend fun clearSession()
}
