package com.croniot.client.domain.repositories

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.session.AppSession
import kotlinx.coroutines.flow.StateFlow

interface AppSessionRepository {

    val session: StateFlow<AppSession>

    suspend fun activateServerSession(account: Account)

    suspend fun activateBleOnlyMode()

    suspend fun clear()
}
