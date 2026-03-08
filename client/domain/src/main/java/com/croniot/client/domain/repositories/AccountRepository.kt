package com.croniot.client.domain.repositories

import com.croniot.client.core.models.Account

interface AccountRepository {
    suspend fun save(account: Account)
    suspend fun get(email: String): Account?
}
