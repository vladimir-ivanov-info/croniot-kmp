package com.server.croniot.data.db.daos

import croniot.models.Account

interface AccountDao {

    fun insert(account: Account): Long

    fun isExistsAccountWithEmail(email: String): Boolean

    fun getAccountEagerSkipTasks(email: String, password: String): Account?

    fun getAll(): List<Account>
}
