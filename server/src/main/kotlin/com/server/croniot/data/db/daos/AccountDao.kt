package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.AccountEntity
import croniot.models.Account

interface AccountDao {

    fun get(email: String): AccountEntity?

    fun insert(account: Account, password: String): Long

    fun isExistsAccountWithEmail(email: String): Boolean

    fun getAccountEagerSkipTasks(email: String): Account?

    fun verifyPassword(email: String, plaintext: String): VerifyPasswordResult

    fun getAll(): List<Account>

    fun isAccountExists(email: String): Boolean

    fun getAccountId(email: String): Long?

    fun getEmailById(accountId: Long): String?
}
