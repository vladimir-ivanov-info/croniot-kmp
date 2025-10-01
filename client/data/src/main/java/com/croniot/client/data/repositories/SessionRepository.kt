package com.croniot.client.data.repositories

import com.croniot.client.core.models.auth.AuthSession
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {

    suspend fun save(session: AuthSession)
    suspend fun clearAllExceptDeviceUuid()

    //suspend fun clearSession()
}