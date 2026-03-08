package com.croniot.client.data.repositories

import com.croniot.client.core.models.Account
import com.croniot.client.domain.repositories.AccountRepository
import com.croniot.client.data.source.local.LocalDatasource

class AccountRepositoryImpl(
    private val localDataSource: LocalDatasource
) : AccountRepository {

    override suspend fun save(account: Account) {
        localDataSource.saveCurrentAccount(account)
    }

    override suspend fun get(email: String): Account? {
        return localDataSource.getCurrentAccount()
    }
}
