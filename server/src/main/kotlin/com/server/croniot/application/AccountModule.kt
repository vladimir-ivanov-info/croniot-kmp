package com.server.croniot.application

import com.croniot.server.db.daos.AccountDao
import com.croniot.server.db.daos.AccountDaoImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class AccountModule {
    @Binds
    @Singleton
    abstract fun bindAccountDao(accountDaoImpl: AccountDaoImpl): AccountDao
}