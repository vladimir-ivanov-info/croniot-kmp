package com.croniot.testing.fakes

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.session.AppSession
import com.croniot.client.domain.repositories.AppSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAppSessionRepository(
    initial: AppSession = AppSession.None,
) : AppSessionRepository {

    private val sessionFlow = MutableStateFlow(initial)

    var clearCalls: Int = 0
        private set

    override val session: StateFlow<AppSession> = sessionFlow

    override suspend fun activateServerSession(account: Account) {
        sessionFlow.value = AppSession.Server(account)
    }

    override suspend fun activateBleOnlyMode() {
        sessionFlow.value = AppSession.BleOnly
    }

    override suspend fun clear() {
        clearCalls++
        sessionFlow.value = AppSession.None
    }
}
