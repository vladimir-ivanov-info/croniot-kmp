package com.croniot.testing.fakes

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.repositories.AccountRepository

class FakeAccountRepository(
    initial: Map<String, Account> = emptyMap(),
) : AccountRepository {

    private val accountsByEmail: MutableMap<String, Account> = initial.toMutableMap()

    var saveCalls: Int = 0
        private set

    override suspend fun save(account: Account) {
        saveCalls++
        accountsByEmail[account.email] = account
    }

    override suspend fun get(email: String): Account? = accountsByEmail[email]
}
