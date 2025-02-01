package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.AccountDao
import croniot.models.Account
import croniot.models.Device
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
) {

    fun isEmailAvailable(email: String): Boolean {
        val exists = accountDao.isExistsAccountWithEmail(email)

        // Add domain-specific logic (if any)
        return !exists // Email is available if it doesn't exist
    }

    fun createAccount(accountUuid: String, nickname: String, email: String, password: String): Long {
        // Perform domain-specific logic (e.g., hashing passwords)
        // val hashedPassword = hashPassword(plainPassword)
        // val account = Account(email = email, password = hashedPassword)
        val account = Account(accountUuid, nickname, email, password, mutableSetOf())
        return accountDao.insert(account)
    }

    fun getAccountEagerSkipTasks(accountEmail: String, accountPassword: String): Account? {
        return accountDao.getAccountEagerSkipTasks(accountEmail, accountPassword)
    }

    // TODO see if this function should be here or in DeviceRepository
    // TODO assicated with /api/account_info, which will probably be deleted
    fun getAccountOfDevice(device: Device): List<Account> {
        // TODO
        return mutableListOf()
    }
}
