package com.croniot.server.db.daos

import croniot.models.Account

interface AccountDao {

    fun insert(account: Account) : Long

    fun isExistsAccountWithEmail(email: String): Boolean

    fun getAccount(email: String, password: String) : Account?

    fun getEmail(accountId: Long) : String?

    fun getAll(): List<Account>

}