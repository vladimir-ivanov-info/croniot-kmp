package com.croniot.client.domain.repositories

import com.croniot.client.domain.models.Account

interface AccountRepository {
    suspend fun save(account: Account)
    suspend fun get(email: String): Account?
}
