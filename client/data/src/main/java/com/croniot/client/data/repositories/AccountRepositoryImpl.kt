package com.croniot.client.data.repositories

import com.croniot.client.domain.models.Account
import com.croniot.client.data.source.local.AuthLocalDatasource
import com.croniot.client.domain.repositories.AccountRepository

class AccountRepositoryImpl(
    private val localDataSource: AuthLocalDatasource
) : AccountRepository {

    override suspend fun save(account: Account) {
        localDataSource.saveCurrentAccount(account)
    }

    override suspend fun get(email: String): Account? {
        return localDataSource.getCurrentAccount()
    }
}
