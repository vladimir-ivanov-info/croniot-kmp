package com.croniot.client.data.source.local

import com.croniot.client.domain.models.Account

interface AuthLocalDatasource {
    suspend fun getCurrentAccount(): Account?
    suspend fun saveCurrentAccount(account: Account?)
    suspend fun saveEmail(email: String)
}
